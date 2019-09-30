package uk.gov.justice.digital.hmpps.pollpush.timed

import com.nhaarman.mockito_kotlin.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import uk.gov.justice.digital.hmpps.pollpush.services.CaseNotesService
import uk.gov.justice.digital.hmpps.pollpush.services.DeliusService

@RunWith(MockitoJUnitRunner::class)
class ProcessCaseNotesTest {
  private val caseNotesService: CaseNotesService = mock()
  private val deliusService: DeliusService = mock()

  private lateinit var process: ProcessCaseNotes

  @Before
  fun before() {
    process = ProcessCaseNotes(caseNotesService, deliusService)
  }

  @Test
  fun `should call case notes and delius`() {
    process.findAndProcessCaseNotes()

    verify(caseNotesService).readAndSaveCaseNotes()
    verify(deliusService, times(2)).retrieveAndPostCaseNotes()
  }

  @Test
  fun `should not allow exceptions to bubble up`() {
    doThrow(RuntimeException("aaaaahhhh")).whenever(deliusService).retrieveAndPostCaseNotes()

    process.findAndProcessCaseNotes()

    verify(caseNotesService, never()).readAndSaveCaseNotes()
    verify(deliusService).retrieveAndPostCaseNotes()
  }
}
