package uk.gov.justice.digital.hmpps.whereabouts.integration.wiremock

import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.google.gson.GsonBuilder

class CaseNotesMockServer : WireMockRule(8093) {
  private val gson = GsonBuilder().create()
}
