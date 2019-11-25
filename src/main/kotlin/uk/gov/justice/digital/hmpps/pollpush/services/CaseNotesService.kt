package uk.gov.justice.digital.hmpps.pollpush.services

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.oauth2.client.OAuth2RestTemplate
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
open class CaseNotesService(@param:Qualifier("caseNotesApiRestTemplate") private val restTemplate: OAuth2RestTemplate) {
  open fun getCaseNote(offenderId: String, caseNoteId: String): CaseNote {
    val response = restTemplate.getForEntity("/case-notes/{offenderId}/{caseNoteId}", CaseNote::class.java, offenderId, caseNoteId)
    return response.body!!
  }
}

data class CaseNote(val eventId: Int,
                    val offenderIdentifier: String,
                    val type: String,
                    val subType: String,
                    val creationDateTime: LocalDateTime,
                    val occurrenceDateTime: LocalDateTime,
                    val authorName: String,
                    val text: String,
                    val locationId: String,
                    val amendments: List<CaseNoteAmendment>) {
  private val dtf: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")

  fun getNoteTextWithAmendments(): String =
      // need format This is a case note ...[PPHILLIPS_ADM updated the case notes on 2019/09/04 11:20:11] Amendment to case note ...[PPHILLIPS_ADM updated the case notes on 2019/09/09 09:23:42] another amendment
      text + amendments.joinToString(separator = "") { a ->
        " ...[${a.authorName} updated the case notes on ${dtf.format(a.creationDateTime)}] ${a.additionalNoteText}"
      }

  fun getAuthorNameWithComma(): String =
      // delius will throw a 400 bad request if it can't find a comma in the author name
      if (authorName.contains(',')) authorName
      else
      // didn't find a comma, so split and change from forename surname to surname, forename
        "${authorName.substringAfterLast(" ")}, ${authorName.substringBeforeLast(" ")}"

  fun calculateModicationDateTime(): LocalDateTime =
      if (amendments.isEmpty()) creationDateTime
      else amendments.sortedBy { creationDateTime }.last().creationDateTime
}

data class CaseNoteAmendment(val creationDateTime: LocalDateTime, val authorName: String, val additionalNoteText: String)
