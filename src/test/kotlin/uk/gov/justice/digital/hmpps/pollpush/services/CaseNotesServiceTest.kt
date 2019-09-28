package uk.gov.justice.digital.hmpps.pollpush.services

import com.nhaarman.mockito_kotlin.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.boot.web.client.RootUriTemplateHandler
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.client.OAuth2RestTemplate
import uk.gov.justice.digital.hmpps.pollpush.dto.CaseNoteEvent
import uk.gov.justice.digital.hmpps.pollpush.dto.CaseNoteEvents
import uk.gov.justice.digital.hmpps.pollpush.repository.*
import java.net.URI
import java.time.LocalDateTime
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class CaseNotesServiceTest {
  private val caseNotesRepository: CaseNotesRepository = mock()
  private val timeStampsRepository: TimeStampsRepository = mock()
  private val restTemplate: OAuth2RestTemplate = mock()

  private lateinit var service: CaseNotesService

  @Before
  fun before() {
    service = CaseNotesService(restTemplate, 10, "NEG, POS", caseNotesRepository, timeStampsRepository)
  }

  @Test
  fun `should call case notes with date from repository`() {
    whenever(timeStampsRepository.findById(anyString())).thenReturn(Optional.of(TimeStamps("id", "2019-03-23T11:22:00")))
    whenever(restTemplate.uriTemplateHandler).thenReturn(RootUriTemplateHandler("http://root"))
    whenever(restTemplate.exchange(any<URI>(), any(), isNull(), any<ParameterizedTypeReference<CaseNoteEvents>>()))
        .thenReturn(ResponseEntity<CaseNoteEvents>(CaseNoteEvents(listOf(createEvent()), LocalDateTime.now().plusMinutes(1)), HttpStatus.OK))

    service.readAndSaveCaseNotes()

    verify(restTemplate).exchange<CaseNoteEvents>(check<URI> {
      assertThat(it.toString()).isEqualTo("http://root/case-notes/events?createdDate=2019-03-23T11:22&limit=10&type=NEG&type=POS")
    }, eq(HttpMethod.GET), isNull(), any<ParameterizedTypeReference<CaseNoteEvents>>())
  }

  @Test
  fun `should handle no body gracefully`() {
    whenever(timeStampsRepository.findById(anyString())).thenReturn(Optional.of(TimeStamps("id", "2019-03-23T11:22:00")))
    whenever(restTemplate.uriTemplateHandler).thenReturn(RootUriTemplateHandler("http://root"))
    whenever(restTemplate.exchange(any<URI>(), any(), isNull(), any<ParameterizedTypeReference<CaseNoteEvents>>()))
        .thenReturn(ResponseEntity<CaseNoteEvents>(HttpStatus.OK))

    service.readAndSaveCaseNotes()

    // will go straight to exception catch block
    verify(caseNotesRepository, never()).saveAll(any())
    verify(timeStampsRepository, never()).save(any())
  }

  @Test
  fun `will call save if result found`() {
    whenever(timeStampsRepository.findById(anyString())).thenReturn(Optional.of(TimeStamps("id", "2019-03-23T11:22:00")))
    whenever(restTemplate.uriTemplateHandler).thenReturn(RootUriTemplateHandler("http://root"))
    val latestEventDate = LocalDateTime.now().plusMinutes(1)
    whenever(restTemplate.exchange(any<URI>(), any(), isNull(), any<ParameterizedTypeReference<CaseNoteEvents>>()))
        .thenReturn(ResponseEntity<CaseNoteEvents>(CaseNoteEvents(listOf(createEvent()), latestEventDate), HttpStatus.OK))

    service.readAndSaveCaseNotes()

    verify(caseNotesRepository).saveAll<CaseNotes>(check {
      assertThat(it).hasSize(1)
      val (_, header, body) = it.elementAt(0)
      assertThat(header).isEqualTo(CaseNoteHeader("12345", "noteId"))
      assertThat(body).isEqualTo(CaseNoteBody(
          noteType = "NEG IEP_WARN",
          content = "note content",
          contactTimeStamp = "2019-03-23T11:22:00.000Z",
          systemTimeStamp = "2019-04-16T11:22:33.000Z",
          staffName = "Some Name",
          establishmentCode = "LEI"))
    })
    verify(timeStampsRepository).save(TimeStamps("pullReceived", latestEventDate.toString()))
  }

  @Test
  fun `will make multiple calls to retrieve all the case notes`() {
    whenever(timeStampsRepository.findById(anyString())).thenReturn(Optional.of(TimeStamps("id", "2019-03-23T11:22:00")))
    whenever(restTemplate.uriTemplateHandler).thenReturn(RootUriTemplateHandler("http://root"))
    whenever(restTemplate.exchange(any<URI>(), any(), isNull(), any<ParameterizedTypeReference<CaseNoteEvents>>()))
        .thenReturn(ResponseEntity<CaseNoteEvents>(CaseNoteEvents(listOf(createEvent()), LocalDateTime.now().minusDays(2)), HttpStatus.OK))
        .thenReturn(ResponseEntity<CaseNoteEvents>(CaseNoteEvents(listOf(createEvent()), LocalDateTime.now().minusDays(1)), HttpStatus.OK))
        .thenReturn(ResponseEntity<CaseNoteEvents>(CaseNoteEvents(listOf(createEvent()), LocalDateTime.now().plusMinutes(1)), HttpStatus.OK))

    service.readAndSaveCaseNotes()

    verify(caseNotesRepository, times(3)).saveAll(any())
    verify(timeStampsRepository, times(3)).save(any())
  }

  private fun createEvent(): CaseNoteEvent = CaseNoteEvent(
      nomsId = "12345",
      id = "noteId",
      content = "note content",
      contactTimestamp = LocalDateTime.parse("2019-03-23T11:22"),
      notificationTimestamp = LocalDateTime.parse("2019-04-16T11:22:33"),
      staffName = "Some Name",
      establishmentCode = "LEI",
      noteType = "NEG IEP_WARN"
  )
}
