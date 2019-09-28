package uk.gov.justice.digital.hmpps.pollpush.repository

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.repository.MongoRepository
import uk.gov.justice.digital.hmpps.pollpush.dto.CaseNoteEvent
import java.time.format.DateTimeFormatter


interface CaseNotesRepository : MongoRepository<CaseNotes, String>

data class CaseNotes(@Id val id: String? = null, val header: CaseNoteHeader, val body: CaseNoteBody) {
  companion object {
    // This is rubbish, but that's how nomis-api did it so we have replicated it here so we send the same data to delius
    // Quoted "Z" to indicate UTC, no timezone offset
    val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  }

  constructor(e: CaseNoteEvent) : this(header = CaseNoteHeader(e.nomsId, e.id),
      body = CaseNoteBody(
          noteType = e.noteType,
          content = e.content,
          contactTimeStamp = dtf.format(e.contactTimestamp),
          systemTimeStamp = dtf.format(e.notificationTimestamp),
          staffName = e.staffName,
          establishmentCode = e.establishmentCode))
}

data class CaseNoteHeader(val nomisId: String, val noteId: String)
data class CaseNoteBody(val noteType: String,
                        val content: String,
                        val contactTimeStamp: String,
                        val systemTimeStamp: String,
                        val staffName: String,
                        val establishmentCode: String)
