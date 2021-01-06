package uk.gov.justice.digital.hmpps.pollpush.services

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class AuthExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val authApi = AuthApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    authApi.start()
    authApi.stubGrantToken()
  }

  override fun beforeEach(context: ExtensionContext) {
    authApi.resetAll()
    authApi.stubGrantToken()
  }

  override fun afterAll(context: ExtensionContext) {
    authApi.stop()
  }
}

class AuthApiMockServer : WireMockServer(wireMockConfig().port(8090).usingFilesUnderClasspath("auth")) {

  fun stubGrantToken() {
    stubFor(
      WireMock.post(WireMock.urlEqualTo("/auth/oauth/token"))
        .willReturn(
          WireMock.aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """{
                    "token_type": "bearer",
                    "access_token": "ABCDE"
                }
              """.trimIndent()
            )
        )
    )
  }
}
