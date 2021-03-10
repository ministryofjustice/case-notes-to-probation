package uk.gov.justice.digital.hmpps.pollpush.resources

import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.pollpush.integration.IntegrationTest

class QueueAdminResourceIntTest : IntegrationTest() {

  @Nested
  @TestInstance(PER_CLASS)
  inner class SecureEndpoints {
    private fun secureEndpoints() =
      listOf(
        "/queue-admin/purge-dlq",
        "/queue-admin/transfer-dlq",
      )

    @ParameterizedTest
    @MethodSource("secureEndpoints")
    internal fun `requires a valid authentication token`(uri: String) {
      webTestClient.put()
        .uri(uri)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isUnauthorized
    }

    @ParameterizedTest
    @MethodSource("secureEndpoints")
    internal fun `requires the correct role`(uri: String) {
      webTestClient.put()
        .uri(uri)
        .headers(setAuthorisation())
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isForbidden
    }

    @ParameterizedTest
    @MethodSource("secureEndpoints")
    internal fun `works with correct role`(uri: String) {
      webTestClient.put()
        .uri(uri)
        .headers(setAuthorisation(roles = listOf("ROLE_CASE_NOTE_QUEUE_ADMIN")))
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk
    }
  }

  @Nested
  inner class ServiceCalls {

    @Test
    internal fun `calls DLQ transfer service`() {
      webTestClient.put()
        .uri("/queue-admin/transfer-dlq")
        .headers(setAuthorisation(roles = listOf("ROLE_CASE_NOTE_QUEUE_ADMIN")))
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk

      verify(queueAdminService).transferDlqMessages()
    }

    @Test
    internal fun `calls DLQ purge service`() {
      webTestClient.put()
        .uri("/queue-admin/purge-dlq")
        .headers(setAuthorisation(roles = listOf("ROLE_CASE_NOTE_QUEUE_ADMIN")))
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk

      verify(queueAdminService).clearAllDlqMessages()
    }
  }
}
