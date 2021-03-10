package uk.gov.justice.digital.hmpps.pollpush.services

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.pollpush.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.pollpush.services.CaseNotesExtension.Companion.caseNotesApi
import java.time.LocalDateTime

class CaseNotesServiceIntTest : IntegrationTest() {

  @Autowired
  private lateinit var service: CaseNotesService

  @Test
  fun `get case note calls community API`() {
    caseNotesApi.stubFor(
      WireMock.get(urlMatching(".*"))
        .willReturn(
          WireMock.aResponse().withHeader("Content-type", "application/json")
            .withStatus(200)
            .withBody(createCaseNoteJson())
        )
    )

    val note = service.getCaseNote("AB123D", "1234")

    Assertions.assertThat(note).isEqualTo(createCaseNote())

    caseNotesApi.verify(getRequestedFor(urlMatching("/case-notes/AB123D/1234")))
  }

  private fun createCaseNoteJson() =
    """
    {
      "eventId": 12345,
      "offenderIdentifier": "offenderId",
      "type": "NEG",
      "subType": "IEP_WARN",
      "creationDateTime": "2019-04-16T11:22:33",
      "occurrenceDateTime": "2019-03-23T11:22:00",
      "authorName": "Some Name",
      "text": "note content",
      "locationId": "LEI",
      "amendments": []
   }
    """.trimIndent()

  private fun createCaseNote() = CaseNote(
    eventId = 12345,
    offenderIdentifier = "offenderId",
    type = "NEG",
    subType = "IEP_WARN",
    creationDateTime = LocalDateTime.parse("2019-04-16T11:22:33"),
    occurrenceDateTime = LocalDateTime.parse("2019-03-23T11:22:00"),
    authorName = "Some Name",
    text = "note content",
    locationId = "LEI",
    amendments = listOf()
  )
}
