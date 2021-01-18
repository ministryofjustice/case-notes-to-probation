package uk.gov.justice.digital.hmpps.pollpush.services

import com.amazonaws.services.sqs.AmazonSQS
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.exactly
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.pollpush.services.CaseNotesExtension.Companion.caseNotesApi
import uk.gov.justice.digital.hmpps.pollpush.services.CommunityApiExtension.Companion.communityApi
import uk.gov.justice.digital.hmpps.pollpush.services.health.IntegrationTest
import wiremock.org.apache.http.protocol.HTTP.CONTENT_TYPE

@ActiveProfiles("z-no-queues")
class CaseNoteListenerPusherIntTest : IntegrationTest() {
  @Autowired
  private lateinit var pusher: CaseNoteListenerPusher

  @Suppress("unused")
  @MockBean
  @Qualifier("awsSqsClient")
  private lateinit var awsSqsClient: AmazonSQS

  @Suppress("unused")
  @MockBean
  @Qualifier("awsSqsDlqClient")
  private lateinit var awsSqsDlqClient: AmazonSQS

  private fun caseNoteEvent(offenderIdDisplay: String = "G4803UT", eventType: String = "KA-KE", agency: String = "MDI") =
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

  @Test
  fun `not found in delius should be ignored`() {
    caseNotesApi.stubFor(
      get(urlMatching("/case-notes/([A-Z0-9]*)/([0-9-]*)"))
        .willReturn(aResponse().withStatus(200).withHeader(CONTENT_TYPE, "application/json").withBody(caseNote()))
    )
    communityApi.stubFor(
      put(urlMatching("/secure/nomisCaseNotes/([A-Z0-9]*)/([0-9-]*)"))
        .willReturn(aResponse().withStatus(404))
    )

    pusher.pushCaseNoteToDelius(caseNoteEvent())

    caseNotesApi.verify(getRequestedFor(urlMatching("/case-notes/G4803UT/1234")))
    communityApi.verify(putRequestedFor(urlPathEqualTo("/secure/nomisCaseNotes/G4803UT/-25")))
  }

  @Test
  fun `service errors in delius should be thrown`() {
    caseNotesApi.stubFor(
      get(urlMatching("/case-notes/([A-Z0-9]*)/([0-9-]*)"))
        .willReturn(aResponse().withStatus(200).withHeader(CONTENT_TYPE, "application/json").withBody(caseNote()))
    )
    communityApi.stubFor(
      put(urlMatching("/secure/nomisCaseNotes/([A-Z0-9]*)/([0-9-]*)"))
        .willReturn(aResponse().withStatus(503))
    )

    assertThatThrownBy { pusher.pushCaseNoteToDelius(caseNoteEvent()) }
      .isInstanceOf(WebClientResponseException.ServiceUnavailable::class.java)
  }

  @Test
  fun `case note not found does not throw an exception or call delius`() {
    caseNotesApi.stubFor(
      get(urlMatching("/case-notes/N4803NF/1234"))
        .willReturn(
          aResponse().withHeader("Content-type", "application/json")
            .withStatus(404)
            .withBody("{\"status\":\"404\",\"developerMessage\":\"case note not found\"}")
        )
    )

    pusher.pushCaseNoteToDelius(caseNoteEvent("N4803NF"))

    caseNotesApi.verify(getRequestedFor(urlMatching("/case-notes/N4803NF/1234")))
    communityApi.verify(exactly(0), putRequestedFor(urlMatching("/secure/nomisCaseNotes/.*")))
  }

  @Nested
  inner class IgnoreKnownDeliusErrors {

    @Test
    fun `NSI case note type`() {
      caseNotesApi.stubFor(
        get(urlMatching("/case-notes/([A-Z0-9]*)/([0-9-]*)"))
          .willReturn(
            aResponse().withStatus(200).withHeader(CONTENT_TYPE, "application/json")
              .withBody(caseNote(eventType = "OMIC_OPD-TRI_CONT"))
          )
      )
      communityApi.stubFor(
        put(urlMatching("/secure/nomisCaseNotes/([A-Z0-9]*)/([0-9-]*)"))
          .willReturn(aResponse().withStatus(500))
      )

      pusher.pushCaseNoteToDelius(caseNoteEvent(eventType = "OMIC_OPD-TRI_CONT"))

      caseNotesApi.verify(getRequestedFor(urlMatching("/case-notes/G4803UT/1234")))
      communityApi.verify(putRequestedFor(urlMatching("/secure/nomisCaseNotes/G4803UT/-25")))
    }

    @Test
    fun `Missing probation area for FYI`() {
      caseNotesApi.stubFor(
        get(urlMatching("/case-notes/([A-Z0-9]*)/([0-9-]*)"))
          .willReturn(
            aResponse().withStatus(200).withHeader(CONTENT_TYPE, "application/json")
              .withBody(caseNote(agency = "FYI"))
          )
      )
      communityApi.stubFor(
        put(urlMatching("/secure/nomisCaseNotes/([A-Z0-9]*)/([0-9-]*)"))
          .willReturn(aResponse().withStatus(400))
      )

      pusher.pushCaseNoteToDelius(caseNoteEvent(agency = "FYI"))

      caseNotesApi.verify(getRequestedFor(urlMatching("/case-notes/G4803UT/1234")))
      communityApi.verify(putRequestedFor(urlMatching("/secure/nomisCaseNotes/G4803UT/-25")))
    }

    @Test
    fun `Missing probation area for TRN`() {
      caseNotesApi.stubFor(
        get(urlMatching("/case-notes/([A-Z0-9]*)/([0-9-]*)"))
          .willReturn(
            aResponse().withStatus(200).withHeader(CONTENT_TYPE, "application/json")
              .withBody(caseNote(agency = "TRN"))
          )
      )
      communityApi.stubFor(
        put(urlMatching("/secure/nomisCaseNotes/([A-Z0-9]*)/([0-9-]*)"))
          .willReturn(aResponse().withStatus(400))
      )

      pusher.pushCaseNoteToDelius(caseNoteEvent(agency = "TRN"))

      caseNotesApi.verify(getRequestedFor(urlMatching("/case-notes/G4803UT/1234")))
      communityApi.verify(putRequestedFor(urlMatching("/secure/nomisCaseNotes/G4803UT/-25")))
    }
  }
}
