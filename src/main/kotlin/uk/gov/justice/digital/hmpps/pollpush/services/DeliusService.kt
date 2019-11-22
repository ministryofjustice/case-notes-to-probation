package uk.gov.justice.digital.hmpps.pollpush.services

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.time.format.DateTimeFormatter

@Service
open class DeliusService(@param:Qualifier("deliusApiRestTemplate") private val restTemplate: RestTemplate) {
  open fun postCaseNote(caseNote: DeliusCaseNote) {
    val (header, body) = caseNote
    restTemplate.put("/secure/nomisCaseNotes/{nomsId}/{caseNoteId}", body, header.nomisId, header.noteId)
  }
}

data class DeliusCaseNote(val header: CaseNoteHeader, val body: CaseNoteBody) {
  companion object {
    // This is rubbish, but that's how nomis-api did it so we have replicated it here so we send the same data to delius
    // Quoted "Z" to indicate UTC, no timezone offset
    val dtf: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  }

  constructor(cn: CaseNote) : this(header = CaseNoteHeader(cn.offenderIdentifier, cn.eventId),
      body = CaseNoteBody(
          noteType = "${cn.type} ${cn.subType}",
          content = cn.getNoteTextWithAmendments(),
          contactTimeStamp = dtf.format(cn.occurrenceDateTime),
          systemTimeStamp = dtf.format(cn.creationDateTime),
          staffName = cn.getAuthorNameWithComma(),
          establishmentCode = cn.locationId))
}

data class CaseNoteHeader(val nomisId: String, val noteId: Int)
data class CaseNoteBody(val noteType: String,
                        val content: String,
                        val contactTimeStamp: String,
                        val systemTimeStamp: String,
                        val staffName: String,
                        val establishmentCode: String)
