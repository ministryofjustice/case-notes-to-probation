package uk.gov.justice.digital.hmpps.pollpush.services

import com.amazonaws.services.sqs.AmazonSQS
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.exactly
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.pollpush.services.CaseNotesExtension.Companion.caseNotesApi
import uk.gov.justice.digital.hmpps.pollpush.services.DeliusExtension.Companion.communityApi
import uk.gov.justice.digital.hmpps.pollpush.services.health.IntegrationTest

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

  private fun validCaseNoteEvent(offenderIdDisplay: String = "G4803UT") =
    """{
    "MessageId": "ae06c49e-1f41-4b9f-b2f2-dcca610d02cd", "Type": "Notification", "Timestamp": "2019-10-21T14:01:18.500Z", 
    "Message": 
      "{\"eventId\":\"5958295\",\"eventType\":\"KA-KE\",\"eventDatetime\":\"2019-10-21T15:00:25.489964\",
      \"rootOffenderId\":2419065,\"offenderIdDisplay\":\"$offenderIdDisplay\",\"agencyLocationId\":\"MDI\", \"caseNoteId\": 1234}", 
    "TopicArn": "arn:aws:sns:eu-west-1:000000000000:offender_events", 
    "MessageAttributes": {"eventType": {"Type": "String", "Value": "KA-KE"}, 
    "id": {"Type": "String", "Value": "8b07cbd9-0820-0a0f-c32f-a9429b618e0b"}, 
    "contentType": {"Type": "String", "Value": "text/plain;charset=UTF-8"}, 
    "timestamp": {"Type": "Number.java.lang.Long", "Value": "1571666478344"}}}
    """.trimIndent()

  @Test
  fun `not found in delius should be ignored`() {
    communityApi.stubFor(
      put(urlMatching("/secure/nomisCaseNotes/([A-Z0-9]*)/([0-9-]*)"))
        .willReturn(aResponse().withStatus(404))
    )

    pusher.pushCaseNoteToDelius(validCaseNoteEvent())

    communityApi.verify(putRequestedFor(urlPathEqualTo("/secure/nomisCaseNotes/A1234AF/-25")))
  }

  @Test
  fun `service errors in delius should be thrown`() {
    communityApi.stubFor(
      put(urlMatching("/secure/nomisCaseNotes/([A-Z0-9]*)/([0-9-]*)"))
        .willReturn(aResponse().withStatus(503))
    )

    assertThatThrownBy { pusher.pushCaseNoteToDelius(validCaseNoteEvent()) }
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

    pusher.pushCaseNoteToDelius(validCaseNoteEvent("N4803NF"))

    communityApi.verify(exactly(0), putRequestedFor(urlMatching("/secure/nomisCaseNotes/.*")))
  }
}
