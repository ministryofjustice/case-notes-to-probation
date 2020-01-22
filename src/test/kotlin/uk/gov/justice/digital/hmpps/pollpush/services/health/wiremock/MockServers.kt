package uk.gov.justice.digital.hmpps.whereabouts.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.google.gson.GsonBuilder

class CaseNotesMockServer : WireMockRule(8083)

class DeliusMockServer : WireMockRule(8096)

class OAuthMockServer : WireMockRule(8090) {
  private val gson = GsonBuilder().create()

  fun stubGrantToken() {
    stubFor(
        WireMock.post(WireMock.urlEqualTo("/auth/oauth/token"))
            .willReturn(WireMock.aResponse()
                .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
                .withBody(gson.toJson(mapOf("access_token" to "ABCDE"))))
    )
  }
}
