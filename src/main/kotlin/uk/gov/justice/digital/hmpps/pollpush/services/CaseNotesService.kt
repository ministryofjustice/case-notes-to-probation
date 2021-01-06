package uk.gov.justice.digital.hmpps.pollpush.services

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class CaseNotesService(
  @Qualifier("authorizedWebClient") private val webClient: WebClient,
  @Value("\${casenotes.endpoint.url}") private val caseNotesApiRootUri: String,
) {
  fun getCaseNote(offenderId: String, caseNoteId: String): CaseNote? =
    try {
      webClient.get()
        .uri("$caseNotesApiRootUri/case-notes/{offenderId}/{caseNoteId}", offenderId, caseNoteId)
        .retrieve()
        .bodyToMono(CaseNote::class.java)
        .onErrorResume(WebClientResponseException::class.java) { emptyWhenNotFound(it) }
        .block()
    } catch (ex: HttpClientErrorException.NotFound) {
      null
    }

  private fun <T> emptyWhenNotFound(exception: WebClientResponseException): Mono<T> = emptyWhen(exception, NOT_FOUND)
  private fun <T> emptyWhen(exception: WebClientResponseException, statusCode: HttpStatus): Mono<T> =
    if (exception.rawStatusCode == statusCode.value()) Mono.empty() else Mono.error(exception)
}

data class CaseNote(
  val eventId: Int,
  val offenderIdentifier: String,
  val type: String,
  val subType: String,
  val creationDateTime: LocalDateTime,
  val occurrenceDateTime: LocalDateTime,
  val authorName: String,
  val text: String,
  val locationId: String,
  val amendments: List<CaseNoteAmendment>
) {
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
    else amendments.mapNotNull { it.creationDateTime }.sorted().lastOrNull() ?: creationDateTime
}

data class CaseNoteAmendment(val creationDateTime: LocalDateTime?, val authorName: String, val additionalNoteText: String)
