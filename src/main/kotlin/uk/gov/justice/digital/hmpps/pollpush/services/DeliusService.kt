package uk.gov.justice.digital.hmpps.pollpush.services

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.time.format.DateTimeFormatter

@Service
class DeliusService(
  @Qualifier("deliusApiRestTemplate") private val restTemplate: RestTemplate,
  @Value("\${delius.enabled}") private val deliusEnabled: Boolean
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun postCaseNote(caseNote: DeliusCaseNote) {
    val (header, body) = caseNote
    if (deliusEnabled) {
      restTemplate.put("/secure/nomisCaseNotes/{nomsId}/{caseNoteId}", body, header.nomisId, header.noteId)
    } else {
      log.info("Delius integration disabled, so not pushing case note ${header.noteId} of type ${body.noteType} for ${header.nomisId}")
    }
  }
}

data class DeliusCaseNote(val header: CaseNoteHeader, val body: CaseNoteBody) {
  companion object {
    // This is rubbish, but that's how nomis-api did it so we have replicated it here so we send the same data to delius
    // Quoted "Z" to indicate UTC, no timezone offset
    private val dtf: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  }

  constructor(cn: CaseNote) : this(
    header = CaseNoteHeader(cn.offenderIdentifier, cn.eventId),
    body = CaseNoteBody(
      noteType = "${cn.type} ${cn.subType}",
      content = cn.getNoteTextWithAmendments(),
      contactTimeStamp = dtf.format(cn.occurrenceDateTime),
      systemTimeStamp = dtf.format(cn.calculateModicationDateTime()),
      staffName = cn.getAuthorNameWithComma(),
      establishmentCode = cn.locationId
    )
  )
}

data class CaseNoteHeader(val nomisId: String, val noteId: Int)
data class CaseNoteBody(
  val noteType: String,
  val content: String,
  val contactTimeStamp: String,
  val systemTimeStamp: String,
  val staffName: String,
  val establishmentCode: String
)
