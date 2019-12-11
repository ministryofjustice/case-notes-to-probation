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
    val note = createCaseNote().copy(text = "HELLO", amendments = listOf(
        CaseNoteAmendment(LocalDateTime.parse("2019-05-01T22:21:20"), "some user", "some amendment"),
        CaseNoteAmendment(LocalDateTime.parse("2019-06-02T22:21:20"), "Another Author", "another amendment")))

    assertThat(note.getNoteTextWithAmendments()).isEqualTo("HELLO ...[some user updated the case notes on 2019/05/01 22:21:20] some amendment ...[Another Author updated the case notes on 2019/06/02 22:21:20] another amendment")
  }

  @Test
  fun `test staff user translated`() {
    assertThat(createCaseNote().getAuthorNameWithComma()).isEqualTo("Name, Some")
  }

  @Test
  fun `test staff user with multiple spaces translated`() {
    assertThat(createCaseNote().copy(authorName = "Some Long Name").getAuthorNameWithComma()).isEqualTo("Name, Some Long")
  }

  @Test
  fun `test staff user with no spaces translated`() {
    assertThat(createCaseNote().copy(authorName = "SomeLongName").getAuthorNameWithComma()).isEqualTo("SomeLongName, SomeLongName")
  }

  @Test
  fun `test staff user in correct format unchanged`() {
    assertThat(createCaseNote().copy(authorName = "Smith, John").getAuthorNameWithComma()).isEqualTo("Smith, John")
  }

  @Test
  fun `test creation date time without amendments`() {
    assertThat(createCaseNote().calculateModicationDateTime()).isEqualTo(LocalDateTime.parse("2019-04-16T11:22:33"))
  }

  @Test
  fun `test creation date time latest amendment`() {
    val note = createCaseNote().copy(amendments = listOf(
        CaseNoteAmendment(LocalDateTime.parse("2019-05-01T22:21:20"), "some user", "some amendment"),
        CaseNoteAmendment(LocalDateTime.parse("2019-06-02T22:21:20"), "Another Author", "another amendment")))
    assertThat(note.calculateModicationDateTime()).isEqualTo(LocalDateTime.parse("2019-06-02T22:21:20"))
  }

  @Test
  fun `test creation date time missing from one amendment`() {
    val note = createCaseNote().copy(amendments = listOf(
        CaseNoteAmendment(LocalDateTime.parse("2019-05-01T22:21:20"), "some user", "some amendment"),
        CaseNoteAmendment(null, "Another Author", "another amendment")))
    assertThat(note.calculateModicationDateTime()).isEqualTo(LocalDateTime.parse("2019-05-01T22:21:20"))
  }

  @Test
  fun `test creation date time missing all amendment`() {
    val note = createCaseNote().copy(amendments = listOf(
        CaseNoteAmendment(null, "some user", "some amendment"),
        CaseNoteAmendment(null, "Another Author", "another amendment")))
    assertThat(note.calculateModicationDateTime()).isEqualTo(LocalDateTime.parse("2019-04-16T11:22:33"))
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
