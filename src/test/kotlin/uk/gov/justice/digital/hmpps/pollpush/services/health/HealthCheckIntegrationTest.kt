package uk.gov.justice.digital.hmpps.pollpush.services.health

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.pollpush.QueueIntegrationTest
import uk.gov.justice.digital.hmpps.pollpush.services.AuthExtension.Companion.authApi
import uk.gov.justice.digital.hmpps.pollpush.services.CaseNotesExtension.Companion.caseNotesApi
import uk.gov.justice.digital.hmpps.pollpush.services.CommunityApiExtension.Companion.communityApi

class HealthCheckIntegrationTest : QueueIntegrationTest() {

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
  fun `Queue Health page reports interesting attributes`() {
    subPing(200)

    webTestClient.get()
      .uri("/health")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("components.events-health.status").isEqualTo("UP")
      .jsonPath("components.events-health.details.queueName").isEqualTo(queueName)
      .jsonPath("components.events-health.details.messagesOnQueue").isEqualTo(0)
      .jsonPath("components.events-health.details.messagesInFlight").isEqualTo(0)
      .jsonPath("components.events-health.details.messagesOnDlq").isEqualTo(0)
      .jsonPath("components.events-health.details.dlqStatus").isEqualTo("UP")
      .jsonPath("components.events-health.details.dlqName").isEqualTo(dlqName ?: "not set")
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
}
