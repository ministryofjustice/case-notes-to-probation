package uk.gov.justice.digital.hmpps.pollpush.repository


import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithAnonymousUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import uk.gov.justice.digital.hmpps.pollpush.dto.CaseNoteEvent
import java.time.LocalDateTime

@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@WithAnonymousUser
open class CaseNotesRepositoryTest {

  @Autowired
  lateinit var caseNotesRepository: CaseNotesRepository

  @Test
  fun `should insert case note`() {
    val (id, _, _) = caseNotesRepository.save(CaseNotes(createEvent()))

    val (_, header, body) = caseNotesRepository.findById(id ?: "missing").orElseThrow { RuntimeException("not found") }

    assertThat(header).isEqualTo(CaseNoteHeader("12345", "noteId"))
    assertThat(body).isEqualTo(CaseNoteBody(
        noteType = "NEG IEP_WARN",
        content = "note content",
        contactTimeStamp = "2019-03-23T11:22:00.000Z",
        systemTimeStamp = "2019-04-16T11:22:33.000Z",
        staffName = "Some Name",
        establishmentCode = "LEI"))
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
