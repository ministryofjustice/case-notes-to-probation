package uk.gov.justice.digital.hmpps.pollpush.services

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.junit.MockitoJUnitRunner
import java.time.LocalDateTime

@RunWith(MockitoJUnitRunner::class)
class CaseNoteListenerPusherTest {
  private val caseNotesService: CaseNotesService = mock()
  private val deliusService: DeliusService = mock()

  private lateinit var pusher: CaseNoteListenerPusher

  private val validCaseNoteEvent = """{
    "MessageId": "ae06c49e-1f41-4b9f-b2f2-dcca610d02cd", "Type": "Notification", "Timestamp": "2019-10-21T14:01:18.500Z", 
    "Message": 
      "{\"eventId\":\"5958295\",\"eventType\":\"KA-KE\",\"eventDatetime\":\"2019-10-21T15:00:25.489964\",
      \"rootOffenderId\":2419065,\"offenderIdDisplay\":\"G4803UT\",\"agencyLocationId\":\"MDI\", \"caseNoteId\": 1234}", 
    "TopicArn": "arn:aws:sns:eu-west-1:000000000000:offender_events", 
    "MessageAttributes": {"eventType": {"Type": "String", "Value": "KA-KE"}, 
    "id": {"Type": "String", "Value": "8b07cbd9-0820-0a0f-c32f-a9429b618e0b"}, 
    "contentType": {"Type": "String", "Value": "text/plain;charset=UTF-8"}, 
    "timestamp": {"Type": "Number.java.lang.Long", "Value": "1571666478344"}}}""".trimIndent()

  @Before
  fun before() {
    pusher = CaseNoteListenerPusher(caseNotesService, deliusService)
  }

  @Test
  fun `case note service called with hydrated event`() {
    whenever(caseNotesService.getCaseNote(anyString(), anyString())).thenReturn(createCaseNote())
    pusher.pushCaseNoteToDelius(validCaseNoteEvent)
    verify(caseNotesService).getCaseNote("G4803UT", "1234")
  }

  @Test
  fun `delius service called with case note from case notes service`() {
    whenever(caseNotesService.getCaseNote(anyString(), anyString())).thenReturn(createCaseNote())
    pusher.pushCaseNoteToDelius(validCaseNoteEvent)
    verify(deliusService).postCaseNote(createDeliusCaseNote())
  }

  private fun createCaseNote() = CaseNote(
      eventId = 1234,
      offenderIdentifier = "offenderId",
      type = "NEG",
      subType = "IEP_WARN",
      creationDateTime = LocalDateTime.parse("2019-04-16T11:22:33"),
      occurrenceDateTime = LocalDateTime.parse("2019-03-23T11:22:00"),
      authorName = "Some Name",
      text = "note content",
      locationId = "LEI",
      amendments = listOf())

  private fun createDeliusCaseNote() = DeliusCaseNote(
      header = CaseNoteHeader("offenderId", 1234),
      body = CaseNoteBody(
          noteType = "NEG IEP_WARN",
          content = "note content",
          contactTimeStamp = "2019-03-23T11:22:00.000Z",
          systemTimeStamp = "2019-04-16T11:22:33.000Z",
          staffName = "Some Name",
          establishmentCode = "LEI"))
}
