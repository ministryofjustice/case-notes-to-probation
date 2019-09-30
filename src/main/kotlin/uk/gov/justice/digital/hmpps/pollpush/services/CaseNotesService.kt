package uk.gov.justice.digital.hmpps.pollpush.services

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.security.oauth2.client.OAuth2RestTemplate
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.pollpush.dto.CaseNoteEvents
import uk.gov.justice.digital.hmpps.pollpush.repository.CaseNotes
import uk.gov.justice.digital.hmpps.pollpush.repository.CaseNotesRepository
import uk.gov.justice.digital.hmpps.pollpush.repository.TimeStamps
import uk.gov.justice.digital.hmpps.pollpush.repository.TimeStampsRepository
import java.time.LocalDateTime
import javax.transaction.Transactional

@Service
open class CaseNotesService(@param:Qualifier("caseNotesApiRestTemplate") private val restTemplate: OAuth2RestTemplate,
                            @param:Value("\${pull.limit:5000}") private val limit: Int,
                            @Value("\${pull.note.types}") noteTypesAsString: String,
                            private val caseNotesRepository: CaseNotesRepository,
                            private val timeStampsRepository: TimeStampsRepository) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  private val noteTypes = StringUtils.split(StringUtils.trim(noteTypesAsString), ", ")

  @Transactional
  open fun readAndSaveCaseNotes() {
    val beforeReadingCaseNotes = LocalDateTime.now()

    try {
      val pullReceived = timeStampsRepository.findById("pullReceived").map { LocalDateTime.parse(it.value) }.orElse(beforeReadingCaseNotes)
      log.info("Pull received last time is {}", pullReceived)

      do {
        val (events, latestEventDate) = getCaseNoteEvents(pullReceived)
        log.info("Found {} events with latest date of {}", events.size, latestEventDate)
        if (events.isNotEmpty()) caseNotesRepository.saveAll(events.map(::CaseNotes))
        timeStampsRepository.save(TimeStamps("pullReceived", latestEventDate.toString()))

      } while (beforeReadingCaseNotes.isAfter(latestEventDate))
    } catch (e: Exception) {
      log.warn("Caught error {} during reading case notes ", e.javaClass.name, e)
      // can't do any more, don't want to miss out on any notes so just hope it is a transient issue
    }
  }

  private fun getCaseNoteEvents(createdDate: LocalDateTime): CaseNoteEvents {
    // bit naff, but the template handler holds the root uri that we need, so have to create a uri from it to then pass to the builder
    val normalisedUri = restTemplate.uriTemplateHandler.expand("/case-notes/events").normalize()

    val uri = UriComponentsBuilder.fromUri(normalisedUri)
        .queryParam("createdDate", createdDate)
        .queryParam("limit", limit)
        .queryParam("type", *noteTypes)
        .build().toUri()

    val response = restTemplate.exchange(uri, HttpMethod.GET, null, object : ParameterizedTypeReference<CaseNoteEvents>() {})
    return response.body!! // let null pointer bubble up
  }
}

