package uk.gov.justice.digital.hmpps.pollpush.services

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class CaseNotesExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val caseNotesApi = CaseNotesApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    caseNotesApi.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    caseNotesApi.resetAll()
  }

  override fun afterAll(context: ExtensionContext) {
    caseNotesApi.stop()
  }
}

class CaseNotesApiMockServer : WireMockServer(wireMockConfig().port(8083).usingFilesUnderClasspath("caseNotes"))
