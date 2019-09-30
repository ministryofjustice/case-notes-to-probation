package uk.gov.justice.digital.hmpps.pollpush.services

import com.nhaarman.mockito_kotlin.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.web.client.RestTemplate
import uk.gov.justice.digital.hmpps.pollpush.repository.*

@RunWith(MockitoJUnitRunner::class)
class DeliusServiceTest {
  private val caseNotesRepository: CaseNotesRepository = mock()
  private val timeStampsRepository: TimeStampsRepository = mock()
  private val restTemplate: RestTemplate = mock()

  private lateinit var service: DeliusService

  @Before
  fun before() {
    service = DeliusService(restTemplate, caseNotesRepository, timeStampsRepository)
  }

  @Test
  fun `should call delius with correct values`() {
    val caseNote = createCaseNote()
    whenever(caseNotesRepository.findAll()).thenReturn(listOf((caseNote)))

    service.retrieveAndPostCaseNotes()

    verify(restTemplate).put("/{nomsId}/{caseNoteId}", caseNote.body, "12345", "noteId")
  }

  @Test
  fun `should delete note after processing`() {
    val caseNote = createCaseNote()
    whenever(caseNotesRepository.findAll()).thenReturn(listOf((caseNote)))

    service.retrieveAndPostCaseNotes()

    verify(caseNotesRepository).delete(caseNote)
  }

  @Test
  fun `should delete note even if saving fails`() {
    val caseNote = createCaseNote()
    whenever(caseNotesRepository.findAll()).thenReturn(listOf((caseNote)))
    doThrow(RuntimeException("something went wrong")).whenever(restTemplate).put(anyString(), any<CaseNotes>(), anyString(), anyString())

    service.retrieveAndPostCaseNotes()

    verify(caseNotesRepository).delete(caseNote)
  }

  @Test
  fun `should then save processing time to mongo`() {
    service.retrieveAndPostCaseNotes()

    verify(timeStampsRepository).save<TimeStamps>(check {
      assertThat(it.id).isEqualTo("pullProcessed")
    })
  }

  private fun createCaseNote(): CaseNotes = CaseNotes(
      header = CaseNoteHeader("12345", "noteId"),
      body = CaseNoteBody(
          noteType = "NEG IEP_WARN",
          content = "note content",
          contactTimeStamp = "2019-03-23T11:22:00.000Z",
          systemTimeStamp = "2019-04-16T11:22:33.000Z",
          staffName = "Some Name",
          establishmentCode = "LEI"))
}
