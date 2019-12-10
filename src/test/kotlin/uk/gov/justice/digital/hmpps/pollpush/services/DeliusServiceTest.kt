package uk.gov.justice.digital.hmpps.pollpush.services

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.security.oauth2.client.OAuth2RestTemplate

@Suppress("DEPRECATION")
@RunWith(MockitoJUnitRunner::class)
class DeliusServiceTest {
  private val restTemplate: OAuth2RestTemplate = mock()

  private lateinit var service: DeliusService
  private lateinit var disabledService: DeliusService

  @Before
  fun before() {
    service = DeliusService(restTemplate, true)
    disabledService = DeliusService(restTemplate, false)
  }

  @Test
  fun `test put case note calls rest template`() {
    val expectedNote = createDeliusCaseNote()

    service.postCaseNote(expectedNote)

    verify(restTemplate).put("/secure/nomisCaseNotes/{nomsId}/{caseNoteId}", expectedNote.body, "AB123D", 1234)
  }

  @Test
  fun `test put case note doesn't call rest template when disabled`() {
    val expectedNote = createDeliusCaseNote()

    disabledService.postCaseNote(expectedNote)

    verify(restTemplate, never()).put(anyString(), any(), anyString(), anyInt())
  }

  private fun createDeliusCaseNote() = DeliusCaseNote(
      header = CaseNoteHeader("AB123D", 1234),
      body = CaseNoteBody(
          noteType = "NEG IEP_WARN",
          content = "note content",
          contactTimeStamp = "2019-03-23T11:22:00.000Z",
          systemTimeStamp = "2019-04-16T11:22:33.000Z",
          staffName = "Some Name",
          establishmentCode = "LEI"))
}
