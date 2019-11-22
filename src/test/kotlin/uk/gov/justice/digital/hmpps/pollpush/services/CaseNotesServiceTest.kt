package uk.gov.justice.digital.hmpps.pollpush.services

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.client.OAuth2RestTemplate
import java.time.LocalDateTime

@RunWith(MockitoJUnitRunner::class)
class CaseNotesServiceTest {
  private val restTemplate: OAuth2RestTemplate = mock()

  private lateinit var service: CaseNotesService

  @Before
  fun before() {
    service = CaseNotesService(restTemplate)
  }

  @Test
  fun `test get case note calls rest template`() {
    val expectedNote = createCaseNote()
    whenever(restTemplate.getForEntity<CaseNote>(anyString(), any(), anyString(), anyString())).thenReturn(ResponseEntity.ok(expectedNote))

    val note = service.getCaseNote("AB123D", "1234")

    assertThat(note).isEqualTo(expectedNote)

    verify(restTemplate).getForEntity("/case-notes/{offenderId}/{caseNoteId}", CaseNote::class.java, "AB123D", "1234")
  }

  @Test
  fun `test amendments built when none exist`() {
    assertThat(createCaseNote().getNoteTextWithAmendments()).isEqualTo("note content")
  }

  @Test
  fun `test amendments built`() {
    val note = CaseNote(
        eventId = -2,
        offenderIdentifier = "offenderId",
        type = "NEG",
        subType = "IEP_WARN",
        creationDateTime = LocalDateTime.parse("2019-04-16T11:22:33"),
        occurrenceDateTime = LocalDateTime.parse("2019-03-23T11:22:00"),
        authorName = "Some Name",
        text = "HELLO",
        locationId = "LEI",
        amendments = listOf(
            CaseNoteAmendment(LocalDateTime.parse("2019-03-01T22:21:20"), "some user", "some amendment"),
            CaseNoteAmendment(LocalDateTime.parse("2019-04-02T22:21:20"), "Another Author", "another amendment")))

    assertThat(note.getNoteTextWithAmendments()).isEqualTo("HELLO ...[some user updated the case notes on 2019/03/01 22:21:20] some amendment ...[Another Author updated the case notes on 2019/04/02 22:21:20] another amendment")
  }

  private fun createCaseNote() = CaseNote(
      eventId = 12345,
      offenderIdentifier = "offenderId",
      type = "NEG",
      subType = "IEP_WARN",
      creationDateTime = LocalDateTime.parse("2019-04-16T11:22:33"),
      occurrenceDateTime = LocalDateTime.parse("2019-03-23T11:22:00"),
      authorName = "Some Name",
      text = "note content",
      locationId = "LEI",
      amendments = listOf())
}
