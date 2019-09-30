package uk.gov.justice.digital.hmpps.pollpush.timed

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.pollpush.services.CaseNotesService
import uk.gov.justice.digital.hmpps.pollpush.services.DeliusService

@Component
class ProcessCaseNotes(val caseNotesService: CaseNotesService, val deliusService: DeliusService) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Scheduled(fixedDelayString = "\${poll.milliseconds}", initialDelayString = "\${random.int[6000,\${poll.milliseconds}]}")
  fun findAndProcessCaseNotes() {
    try {
      // grab any existing notes from mongo and process
      deliusService.retrieveAndPostCaseNotes()

      // read new case notes and process
      caseNotesService.readAndSaveCaseNotes()

      // send them to delius
      deliusService.retrieveAndPostCaseNotes()
    } catch (e: Exception) {
      log.warn("Caught error {} during processing case notes", e.javaClass.name, e)
      // can't do any more, don't want to miss out on any notes so just hope it is a transient issue
    }
  }
}
