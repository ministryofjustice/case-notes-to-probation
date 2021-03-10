package uk.gov.justice.digital.hmpps.pollpush.services.health

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest
import com.amazonaws.services.sqs.model.GetQueueAttributesResult
import com.amazonaws.services.sqs.model.QueueAttributeName
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.nhaarman.mockitokotlin2.whenever
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.test.util.ReflectionTestUtils
import uk.gov.justice.digital.hmpps.pollpush.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.pollpush.services.AuthExtension.Companion.authApi
import uk.gov.justice.digital.hmpps.pollpush.services.CaseNotesExtension.Companion.caseNotesApi
import uk.gov.justice.digital.hmpps.pollpush.services.CommunityApiExtension.Companion.communityApi
import uk.gov.justice.digital.hmpps.pollpush.services.health.QueueAttributes.MESSAGES_IN_FLIGHT
import uk.gov.justice.digital.hmpps.pollpush.services.health.QueueAttributes.MESSAGES_ON_DLQ
import uk.gov.justice.digital.hmpps.pollpush.services.health.QueueAttributes.MESSAGES_ON_QUEUE

class HealthCheckIntegrationTest : IntegrationTest() {
  @Autowired
  private lateinit var queueHealth: QueueHealth

  @SpyBean
  @Qualifier("awsSqsClient")
  private lateinit var awsSqsClient: AmazonSQS

  @Autowired
  @Value("\${sqs.queue.name}")
  private lateinit var queueName: String

  @Autowired
  @Value("\${sqs.dlq.name}")
  private lateinit var dlqName: String

  @AfterEach
  fun tearDown() {
    ReflectionTestUtils.setField(queueHealth, "queueName", queueName)
    ReflectionTestUtils.setField(queueHealth, "dlqName", dlqName)
  }

  @Test
  fun `Health page reports ok`() {
    subPing(200)

    webTestClient.get()
      .uri("/health")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("components.OAuthApiHealth.details.HttpStatus").isEqualTo("OK")
      .jsonPath("components.caseNotesApiHealth.details.HttpStatus").isEqualTo("OK")
      .jsonPath("components.communityApiHealth.details.HttpStatus").isEqualTo("OK")
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `Health ping page is accessible`() {
    webTestClient.get()
      .uri("/health/ping")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `Health liveness page is accessible`() {
    webTestClient.get()
      .uri("/health/liveness")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `Health readiness page is accessible`() {
    webTestClient.get()
      .uri("/health/readiness")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `Health page reports down`() {
    subPing(404)

    webTestClient.get()
      .uri("/health")
      .exchange()
      .expectStatus()
      .is5xxServerError
      .expectBody()
      .jsonPath("status").isEqualTo("DOWN")
      .jsonPath("components.OAuthApiHealth.details.HttpStatus").isEqualTo("NOT_FOUND")
      .jsonPath("components.OAuthApiHealth.details.error").value(containsString("Exception"))
      .jsonPath("components.OAuthApiHealth.details.error").value(containsString("404 Not Found"))
      .jsonPath("components.caseNotesApiHealth.details.HttpStatus").isEqualTo("NOT_FOUND")
      .jsonPath("components.caseNotesApiHealth.details.error").value(containsString("Exception"))
      .jsonPath("components.caseNotesApiHealth.details.error").value(containsString("404 Not Found"))
      .jsonPath("components.communityApiHealth.details.HttpStatus").isEqualTo("NOT_FOUND")
      .jsonPath("components.communityApiHealth.details.error").value(containsString("Exception"))
      .jsonPath("components.communityApiHealth.details.error").value(containsString("404 Not Found"))
  }

  @Test
  fun `Health page reports a teapot`() {
    subPing(418)

    webTestClient.get()
      .uri("/health")
      .exchange()
      .expectStatus()
      .is5xxServerError
      .expectBody()
      .jsonPath("status").isEqualTo("DOWN")
      .jsonPath("components.OAuthApiHealth.details.HttpStatus").isEqualTo("I_AM_A_TEAPOT")
      .jsonPath("components.OAuthApiHealth.details.error").value(containsString("Exception"))
      .jsonPath("components.OAuthApiHealth.details.error").value(containsString("418 I'm a teapot"))
      .jsonPath("components.caseNotesApiHealth.details.HttpStatus").isEqualTo("I_AM_A_TEAPOT")
      .jsonPath("components.caseNotesApiHealth.details.error").value(containsString("Exception"))
      .jsonPath("components.caseNotesApiHealth.details.error").value(containsString("418 I'm a teapot"))
      .jsonPath("components.communityApiHealth.details.HttpStatus").isEqualTo("I_AM_A_TEAPOT")
      .jsonPath("components.communityApiHealth.details.error").value(containsString("Exception"))
      .jsonPath("components.communityApiHealth.details.error").value(containsString("418 I'm a teapot"))
  }

  @Test
  fun `Queue Health page reports ok`() {
    subPing(200)

    webTestClient.get()
      .uri("/health")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("components.queueHealth.status").isEqualTo("UP")
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `Queue Health page reports interesting attributes`() {
    subPing(200)

    webTestClient.get()
      .uri("/health")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("components.queueHealth.details.${MESSAGES_ON_QUEUE.healthName}").isEqualTo(0)
      .jsonPath("components.queueHealth.details.${MESSAGES_IN_FLIGHT.healthName}").isEqualTo(0)
  }

  @Test
  fun `Queue does not exist reports down`() {
    ReflectionTestUtils.setField(queueHealth, "queueName", "missing_queue")
    subPing(200)

    webTestClient.get()
      .uri("/health")
      .exchange()
      .expectStatus()
      .is5xxServerError
      .expectBody()
      .jsonPath("components.queueHealth.status").isEqualTo("DOWN")
      .jsonPath("status").isEqualTo("DOWN")
  }

  @Test
  fun `Queue health ok and dlq health ok, reports everything up`() {
    subPing(200)

    webTestClient.get()
      .uri("/health")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("components.queueHealth.status").isEqualTo("UP")
      .jsonPath("components.queueHealth.details.dlqStatus").isEqualTo(DlqStatus.UP.description)
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `Dlq health reports interesting attributes`() {
    subPing(200)

    webTestClient.get()
      .uri("/health")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("components.queueHealth.details.${MESSAGES_ON_DLQ.healthName}").isEqualTo(0)
  }

  @Test
  fun `Dlq down brings main health and queue health down`() {
    subPing(200)
    mockQueueWithoutRedrivePolicyAttributes()

    webTestClient.get()
      .uri("/health")
      .exchange()
      .expectStatus()
      .is5xxServerError
      .expectBody()
      .jsonPath("components.queueHealth.status").isEqualTo("DOWN")
      .jsonPath("components.queueHealth.details.dlqStatus").isEqualTo(DlqStatus.NOT_ATTACHED.description)
      .jsonPath("status").isEqualTo("DOWN")
  }

  @Test
  fun `Dlq not found reports dlq down`() {
    subPing(200)
    ReflectionTestUtils.setField(queueHealth, "dlqName", "missing_queue")

    webTestClient.get()
      .uri("/health")
      .exchange()
      .expectStatus()
      .is5xxServerError
      .expectBody()
      .jsonPath("components.queueHealth.details.dlqStatus").isEqualTo(DlqStatus.NOT_FOUND.description)
  }

  private fun subPing(status: Int) {
    authApi.stubFor(
      get("/auth/health/ping").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(if (status == 200) """{"status":"UP"}""" else "some error")
          .withStatus(status)
      )
    )

    caseNotesApi.stubFor(
      get("/health/ping").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(if (status == 200) "pong" else "some error")
          .withStatus(status)
      )
    )

    communityApi.stubFor(
      get("/health/ping").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(if (status == 200) "pong" else "some error")
          .withStatus(status)
      )
    )
  }

  private fun mockQueueWithoutRedrivePolicyAttributes() {
    val queueName = ReflectionTestUtils.getField(queueHealth, "queueName") as String
    val queueUrl = awsSqsClient.getQueueUrl(queueName)
    whenever(awsSqsClient.getQueueAttributes(GetQueueAttributesRequest(queueUrl.queueUrl).withAttributeNames(listOf(QueueAttributeName.All.toString()))))
      .thenReturn(GetQueueAttributesResult())
  }
}
