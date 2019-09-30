package uk.gov.justice.digital.hmpps.pollpush.services

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import uk.gov.justice.digital.hmpps.pollpush.repository.CaseNotes
import uk.gov.justice.digital.hmpps.pollpush.repository.CaseNotesRepository
import uk.gov.justice.digital.hmpps.pollpush.repository.TimeStamps
import uk.gov.justice.digital.hmpps.pollpush.repository.TimeStampsRepository
import java.time.LocalDateTime
import javax.transaction.Transactional

@Service
open class DeliusService(@param:Qualifier("deliusApiRestTemplate") private val restTemplate: RestTemplate,
                         private val caseNotesRepository: CaseNotesRepository,
                         private val timeStampsRepository: TimeStampsRepository) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  open fun retrieveAndPostCaseNotes() {
    // Get all notes from mongo
    val notes = caseNotesRepository.findAll()
    log.info("Found {} notes", notes.size)

    notes.forEach {
      // send to delius
      try {
        postCaseNote(it)
      } catch (e: Exception) {
        // Follow existing scala app in that a failure to push the note is ignored
        log.warn("Caught exception {} during sending to delius", e.javaClass.name, e)
        log.warn("Failed to send to delius for note {}, ignoring", it.header.noteId)
      }
      // then remove after processing
      caseNotesRepository.delete(it)
      log.info("Processed note {}", it.header.noteId)
    }

    // record completed
    timeStampsRepository.save(TimeStamps("pullProcessed", LocalDateTime.now().toString()))

    log.info("Processed {} records", notes.size)
  }

  private fun postCaseNote(caseNote: CaseNotes) {
    val (_, header, body) = caseNote

    restTemplate.put("/{nomsId}/{caseNoteId}", body, header.nomisId, header.noteId)
  }
}

