package uk.gov.justice.digital.hmpps.pollpush.resources

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.pollpush.services.health.IntegrationTest

class QueueAdminResourceIntTest : IntegrationTest() {

  private fun secureEndpoints() =
    listOf(
      "/queue-admin/purge-dlq",
      "/queue-admin/transfer-dlq",
    )

  @ParameterizedTest
  @MethodSource("secureEndpoints")
  fun `requires a valid JWT token`(url: String) {
    restTemplate.put(url, null)
  }

}