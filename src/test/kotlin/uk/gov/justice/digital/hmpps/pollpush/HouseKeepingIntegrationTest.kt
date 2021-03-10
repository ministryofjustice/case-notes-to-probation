package uk.gov.justice.digital.hmpps.pollpush

import com.github.tomakehurst.wiremock.client.WireMock
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.pollpush.services.CaseNotesExtension.Companion.caseNotesApi
import uk.gov.justice.digital.hmpps.pollpush.services.CommunityApiExtension.Companion.communityApi
import wiremock.org.apache.http.protocol.HTTP

class HouseKeepingIntegrationTest : QueueIntegrationTest() {

  @Test
  fun `will purge any messages on the dlq`() {
    awsSqsDlqClient.sendMessage(getDlqUrl(), "{}")
    await untilCallTo { getNumberOfMessagesCurrentlyOnDlq() } matches { it == 1 }

    webTestClient.put()
      .uri("/queue-admin/purge-dlq")
      .headers(setAuthorisation(roles = listOf("ROLE_CASE_NOTE_QUEUE_ADMIN")))
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk

    await untilCallTo { getNumberOfMessagesCurrentlyOnDlq() } matches { it == 0 }
    assertThat(getNumberOfMessagesCurrentlyOnQueue()).isEqualTo(0)

    // Nothing to process
    caseNotesApi.verify(0, WireMock.anyRequestedFor(WireMock.anyUrl()))
    communityApi.verify(0, WireMock.anyRequestedFor(WireMock.anyUrl()))
  }

  @Test
  fun `housekeeping will consume a message on the dlq and return to main queue`() {
    stubApiCalls()

    awsSqsDlqClient.sendMessage(getDlqUrl(), caseNoteEvent())

    webTestClient.put()
      .uri("/queue-admin/queue-housekeeping")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk

    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }
    await untilCallTo { getNumberOfMessagesCurrentlyOnDlq() } matches { it == 0 }

    caseNotesApi.verify(1, WireMock.getRequestedFor(WireMock.urlMatching("/case-notes/G4803UT/1234")))
    communityApi.verify(1, WireMock.putRequestedFor(WireMock.urlMatching("/secure/nomisCaseNotes/G4803UT/-25")))
  }

  @Test
  fun `will consume a message on the dlq and return to main queue`() {
    stubApiCalls()

    awsSqsDlqClient.sendMessage(getDlqUrl(), caseNoteEvent())

    webTestClient.put()
      .uri("/queue-admin/transfer-dlq")
      .headers(setAuthorisation(roles = listOf("ROLE_CASE_NOTE_QUEUE_ADMIN")))
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk

    await untilCallTo { getNumberOfMessagesCurrentlyOnDlq() } matches { it == 0 }
    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }

    caseNotesApi.verify(1, WireMock.getRequestedFor(WireMock.urlMatching("/case-notes/G4803UT/1234")))
    communityApi.verify(1, WireMock.putRequestedFor(WireMock.urlMatching("/secure/nomisCaseNotes/G4803UT/-25")))
  }
}

private fun stubApiCalls() {
  // stub calls
  caseNotesApi.stubFor(
    WireMock.get(WireMock.urlMatching("/case-notes/G4803UT/1234"))
      .willReturn(
        WireMock.aResponse().withStatus(200).withHeader(HTTP.CONTENT_TYPE, "application/json")
          .withBody(caseNote())
      )
  )
  communityApi.stubFor(
    WireMock.put(WireMock.urlMatching("/secure/nomisCaseNotes/G4803UT/-25"))
      .willReturn(WireMock.aResponse().withStatus(200))
  )
}

private fun caseNoteEvent(offenderIdDisplay: String = "G4803UT", eventType: String = "KA-KE") =
  """{
    "MessageId": "ae06c49e-1f41-4b9f-b2f2-dcca610d02cd", "Type": "Notification", "Timestamp": "2019-10-21T14:01:18.500Z", 
    "Message": 
      "{\"eventId\":\"5958295\",\"eventType\":\"$eventType\",\"eventDatetime\":\"2019-10-21T15:00:25.489964\",
      \"rootOffenderId\":2419065,\"offenderIdDisplay\":\"$offenderIdDisplay\",\"agencyLocationId\":\"MDI\", \"caseNoteId\": 1234}", 
    "TopicArn": "arn:aws:sns:eu-west-1:000000000000:offender_events", 
    "MessageAttributes": {"eventType": {"Type": "String", "Value": "KA-KE"}, 
    "id": {"Type": "String", "Value": "8b07cbd9-0820-0a0f-c32f-a9429b618e0b"}, 
    "contentType": {"Type": "String", "Value": "text/plain;charset=UTF-8"}, 
    "timestamp": {"Type": "Number.java.lang.Long", "Value": "1571666478344"}}}
  """.trimIndent()

private fun caseNote(offenderIdentifier: String = "G4803UT", eventType: String = "KA-KE", agency: String = "MDI") =
  """
      {
        "caseNoteId": "1234",
        "eventId": "-25",
        "offenderIdentifier": "$offenderIdentifier",
        "type": "${eventType.substringBefore("-")}",
        "typeDescription": "POM Notes",
        "subType": "${eventType.substringAfter("-")}",
        "subTypeDescription": "General POM Note",
        "authorUserId": "SECURE_CASENOTE_USER_ID",
        "authorName": "Mikey Mouse",
        "text": "This is a case note",
        "locationId": "$agency",
        "amendments": [],
        "creationDateTime": "2019-03-23T11:22",
        "occurrenceDateTime": "2019-03-23T11:22"
      }
  """.trimIndent()
