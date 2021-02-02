package uk.gov.justice.digital.hmpps.pollpush.services

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.time.format.DateTimeFormatter

@Service
class CommunityApiService(
  @Qualifier("authorizedWebClient") private val webClient: WebClient,
  @Value("\${delius.enabled}") private val deliusEnabled: Boolean,
  @Value("\${delius.endpoint.url}") private val communityApiRootUri: String,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun postCaseNote(caseNote: DeliusCaseNote) {
    val (header, body) = caseNote
    if (deliusEnabled) {
      webClient.put()
        .uri("$communityApiRootUri/secure/nomisCaseNotes/{nomsId}/{caseNoteId}", header.nomisId, header.noteId)
        .bodyValue(body)
        .retrieve()
        .toBodilessEntity()
        .onErrorResume(WebClientResponseException::class.java) { emptyWhenNotFound(it) }
        .onErrorResume(WebClientResponseException::class.java) { emptyWhenConflict(it) }
        .onErrorResume(WebClientResponseException::class.java) { emptyWhenIgnoringDeliusError(it, caseNote) }
        .block()
    } else {
      log.info("Delius integration disabled, so not pushing case note ${header.noteId} of type ${body.noteType} for ${header.nomisId}")
    }
  }

  private fun <T> emptyWhenNotFound(exception: WebClientResponseException): Mono<T> = emptyWhen(exception, NOT_FOUND)
  private fun <T> emptyWhenConflict(exception: WebClientResponseException): Mono<T> = emptyWhen(exception, CONFLICT)
  private fun <T> emptyWhen(exception: WebClientResponseException, statusCode: HttpStatus): Mono<T> =
    if (exception.rawStatusCode == statusCode.value()) Mono.empty() else Mono.error(exception)

  private fun <T> emptyWhenIgnoringDeliusError(
    exception: WebClientResponseException,
    caseNote: DeliusCaseNote
  ): Mono<T> =
    if (ignoreDeliusError(exception, caseNote)) Mono.empty() else Mono.error(exception)

  // TODO We can stop ignoring these errors once they are fixed in Delius - i.e. when we stop receiving the warning log messages
  private fun ignoreDeliusError(exception: WebClientResponseException, caseNote: DeliusCaseNote): Boolean {
    if (exception.rawStatusCode == BAD_REQUEST.value() &&
      listOf("FYI", "TRN", "ZZGHI").contains(caseNote.body.establishmentCode)
    ) {
      log.warn("Ignoring Delius server error because we know Delius cannot handle agency ${caseNote.body.establishmentCode}")
      return true
    }
    if (exception.rawStatusCode == INTERNAL_SERVER_ERROR.value() &&
      caseNote.body.noteType.startsWith("OMIC_OPD")
    ) {
      log.warn("Ignoring Delius server error because we know Delius cannot handle NSI case notes of type ${caseNote.body.noteType}")
      return true
    }
    return false
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
