@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.pollpush.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class CaseNotesServiceTest {

  @Test
  fun `test amendments built when none exist`() {
    assertThat(createCaseNote().getNoteTextWithAmendments()).isEqualTo("note content")
  }

  @Test
  fun `test amendments built`() {
    val note = createCaseNote().copy(
      text = "HELLO",
      amendments = listOf(
        CaseNoteAmendment(ZonedDateTime.parse("2019-05-01T22:21:20+00:00"), "some user", "some amendment"),
        CaseNoteAmendment(ZonedDateTime.parse("2019-06-02T22:21:20+00:00"), "Another Author", "another amendment")
      )
    )

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
    assertThat(createCaseNote().calculateModicationDateTime()).isEqualTo(ZonedDateTime.parse("2019-04-16T11:22:33.000+00:00"))
  }

  @Test
  fun `test creation date time latest amendment`() {
    val note = createCaseNote().copy(
      amendments = listOf(
        CaseNoteAmendment(ZonedDateTime.parse("2019-05-01T22:21:20+00:00"), "some user", "some amendment"),
        CaseNoteAmendment(ZonedDateTime.parse("2019-06-02T22:21:20+00:00"), "Another Author", "another amendment")
      )
    )
    assertThat(note.calculateModicationDateTime()).isEqualTo(ZonedDateTime.parse("2019-06-02T22:21:20+00:00"))
  }

  @Test
  fun `test creation date time missing from one amendment`() {
    val note = createCaseNote().copy(
      amendments = listOf(
        CaseNoteAmendment(ZonedDateTime.parse("2019-05-01T22:21:20+00:00"), "some user", "some amendment"),
        CaseNoteAmendment(null, "Another Author", "another amendment")
      )
    )
    assertThat(note.calculateModicationDateTime()).isEqualTo(ZonedDateTime.parse("2019-05-01T22:21:20+00:00"))
  }

  @Test
  fun `test creation date time missing all amendment`() {
    val note = createCaseNote().copy(
      amendments = listOf(
        CaseNoteAmendment(null, "some user", "some amendment"),
        CaseNoteAmendment(null, "Another Author", "another amendment")
      )
    )
    assertThat(note.calculateModicationDateTime()).isEqualTo(ZonedDateTime.parse("2019-04-16T11:22:33+00:00"))
  }

  private fun createCaseNote() = CaseNote(
    eventId = 12345,
    offenderIdentifier = "offenderId",
    type = "NEG",
    subType = "IEP_WARN",
    creationDateTime = ZonedDateTime.parse("2019-04-16T11:22:33.000+00:00"),
    occurrenceDateTime = ZonedDateTime.parse("2019-03-23T11:22:00.000+00:00"),
    authorName = "Some Name",
    text = "note content",
    locationId = "LEI",
    amendments = listOf()
  )
}
