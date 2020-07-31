package uk.gov.justice.digital.hmpps.pollpush.config

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.client.ClientHttpResponse


class NotFoundAndConflictIgnoringResponseErrorHandlerTest {
  private val errorHandler = NotFoundAndConflictIgnoringResponseErrorHandler()
  private val httpResponse: ClientHttpResponse = mock()

  @Test
  fun `test that 404s are ignored`() {
    whenever(httpResponse.rawStatusCode).thenReturn(404)
    assertThat(errorHandler.hasError(httpResponse)).isFalse()
  }

  @Test
  fun `test that 409s are ignored`() {
    whenever(httpResponse.rawStatusCode).thenReturn(409)
    assertThat(errorHandler.hasError(httpResponse)).isFalse()
  }

  @Test
  fun `test that other client exceptions are handled`() {
    whenever(httpResponse.rawStatusCode).thenReturn(401)
    assertThat(errorHandler.hasError(httpResponse)).isTrue()
  }

  @Test
  fun `test that successes are ignored`() {
    whenever(httpResponse.rawStatusCode).thenReturn(201)
    assertThat(errorHandler.hasError(httpResponse)).isFalse()
  }

  @Test
  fun `test that server errors are handled`() {
    whenever(httpResponse.rawStatusCode).thenReturn(500)
    assertThat(errorHandler.hasError(httpResponse)).isTrue()
  }
}
