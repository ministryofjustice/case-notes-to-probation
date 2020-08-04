@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.pollpush.config

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.client.ClientHttpResponse
import org.springframework.security.oauth2.client.http.OAuth2ErrorHandler
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException


class NotFoundAndConflictIgnoringResponseErrorHandlerTest {
  private val oAuth2ProtectedResourceDetails: OAuth2ProtectedResourceDetails = mock()
  private val errorHandler = OAuth2ErrorHandler(NotFoundAndConflictIgnoringResponseErrorHandler(), oAuth2ProtectedResourceDetails)
  private val httpResponse: ClientHttpResponse = mock()

  @Test
  fun `test that 404s are ignored`() {
    whenever(httpResponse.statusCode).thenReturn(NOT_FOUND)
    whenever(httpResponse.headers).thenReturn(HttpHeaders.EMPTY)
    errorHandler.handleError(httpResponse)
  }

  @Test
  fun `test that 409s are ignored`() {
    whenever(httpResponse.statusCode).thenReturn(CONFLICT)
    whenever(httpResponse.headers).thenReturn(HttpHeaders.EMPTY)
    errorHandler.handleError(httpResponse)
  }

  @Test
  fun `test that other client exceptions are handled`() {
    whenever(httpResponse.statusCode).thenReturn(UNAUTHORIZED)
    whenever(httpResponse.headers).thenReturn(HttpHeaders.EMPTY)
    assertThatThrownBy { errorHandler.handleError(httpResponse) }.isInstanceOf(HttpClientErrorException.Unauthorized::class.java)
  }

  @Test
  fun `test that server errors are handled`() {
    whenever(httpResponse.rawStatusCode).thenReturn(503)
    whenever(httpResponse.statusCode).thenReturn(SERVICE_UNAVAILABLE)
    whenever(httpResponse.headers).thenReturn(HttpHeaders.EMPTY)
    assertThatThrownBy { errorHandler.handleError(httpResponse) }.isInstanceOf(HttpServerErrorException.ServiceUnavailable::class.java)
  }
}
