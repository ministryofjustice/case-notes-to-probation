package uk.gov.justice.digital.hmpps.pollpush.services

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
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
  }

  override fun beforeEach(context: ExtensionContext) {
    authApi.resetAll()
  }

  override fun afterAll(context: ExtensionContext) {
    authApi.stop()
  }
}

class AuthApiMockServer : WireMockServer(wireMockConfig().port(8090).usingFilesUnderClasspath("auth"))
