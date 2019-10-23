package uk.gov.justice.digital.hmpps.pollpush.services.health

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.springframework.http.HttpStatus
import org.springframework.util.MimeType

class PingEndpointIntegrationTest: IntegrationTest() {
  @Test
  fun `ping endpoint responds with pong`() {
    val response = restTemplate.getForEntity("/ping", String::class.java)

    assertThat(response.statusCode).isEqualTo(HttpStatus.OK)

    assertThat(response.body).isEqualTo("pong")
    assertThat(response.headers.contentType).isEqualTo(MimeType.valueOf("text/plain;charset=UTF-8"))
  }
}
