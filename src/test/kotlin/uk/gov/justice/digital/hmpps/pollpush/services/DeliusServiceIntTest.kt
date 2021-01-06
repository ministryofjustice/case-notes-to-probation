package uk.gov.justice.digital.hmpps.pollpush.services

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.util.ReflectionTestUtils
import uk.gov.justice.digital.hmpps.pollpush.services.DeliusExtension.Companion.communityApi
import uk.gov.justice.digital.hmpps.pollpush.services.health.IntegrationTest

@Suppress("DEPRECATION")
class DeliusServiceIntTest : IntegrationTest() {

  @Autowired
  private lateinit var deliusService: DeliusService

  @Test
  fun `test put case note calls rest template`() {
    communityApi.stubFor(
      put(urlMatching("/secure/nomisCaseNotes/AB123D/1234"))
        .willReturn(
          aResponse().withHeader("Content-type", "application/json")
            .withStatus(200)
            .withBody(createDeliusCaseNoteJson())
        )
    )
    val expectedNote = createDeliusCaseNote()

    deliusService.postCaseNote(expectedNote)

    // TODO how to capture the body?
    communityApi.verify(putRequestedFor(urlMatching("/secure/nomisCaseNotes/AB123D/1234")))
  }

  @Test
  fun `test put case note doesn't call rest template when disabled`() {
    ReflectionTestUtils.setField(deliusService, "deliusEnabled", false)

    deliusService.postCaseNote(createDeliusCaseNote())

    communityApi.verify(WireMock.exactly(0), putRequestedFor(urlMatching("/secure/nomisCaseNotes/AB123D/1234")))

    ReflectionTestUtils.setField(deliusService, "deliusEnabled", true)
  }

  private fun createDeliusCaseNoteJson() = """
    {
      "caseNoteHeader": {
        "nomisId": "AB123D",
        "noteId": 1234
      },
      "caseNoteBody": {
        "noteType": "NEG IEP_WARN",                    
        "content": "note content",                     
        "contactTimeStamp": "2019-03-23T11:22:00.000Z",
        "systemTimeStamp": "2019-04-16T11:22:33.000Z", 
        "staffName": "Some Name",                      
        "establishmentCode": "LEI"                     
      }
    }
  """.trimIndent()

  private fun createDeliusCaseNote() = DeliusCaseNote(
    header = CaseNoteHeader("AB123D", 1234),
    body = CaseNoteBody(
      noteType = "NEG IEP_WARN",
      content = "note content",
      contactTimeStamp = "2019-03-23T11:22:00.000Z",
      systemTimeStamp = "2019-04-16T11:22:33.000Z",
      staffName = "Some Name",
      establishmentCode = "LEI"
    )
  )
}
