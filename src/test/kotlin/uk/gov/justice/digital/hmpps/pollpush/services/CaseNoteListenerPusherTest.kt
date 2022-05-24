package uk.gov.justice.digital.hmpps.pollpush.services

import com.google.gson.GsonBuilder
import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class CaseNoteListenerPusherTest {
  private val caseNotesService: CaseNotesService = mock()
  private val communityApiService: CommunityApiService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val gson = GsonBuilder().create()

  private val pusher = CaseNoteListenerPusher(caseNotesService, communityApiService, telemetryClient, gson)

  private val validCaseNoteEvent =
    """{
    "MessageId": "ae06c49e-1f41-4b9f-b2f2-dcca610d02cd", "Type": "Notification", "Timestamp": "2019-10-21T14:01:18.500Z", 
    "Message": 
      "{\"eventId\":\"5958295\",\"eventType\":\"KA-KE\",\"eventDatetime\":\"2019-10-21T15:00:25.489964\",
      \"rootOffenderId\":2419065,\"offenderIdDisplay\":\"G4803UT\",\"agencyLocationId\":\"MDI\", \"caseNoteId\": 1234}", 
    "TopicArn": "arn:aws:sns:eu-west-1:000000000000:offender_events", 
    "MessageAttributes": {"eventType": {"Type": "String", "Value": "KA-KE"}, 
    "id": {"Type": "String", "Value": "8b07cbd9-0820-0a0f-c32f-a9429b618e0b"}, 
    "contentType": {"Type": "String", "Value": "text/plain;charset=UTF-8"}, 
    "timestamp": {"Type": "Number.java.lang.Long", "Value": "1571666478344"}}}
    """.trimIndent()

  private val invalidCaseNoteEvent =
    """{
    "Type" : "Notification",
    "MessageId" : "bb282d3c-71fd-58a2-b973-7b582f2f54a4",
    "TopicArn" : "arn:aws:sns:eu-west-2:joe:cloud-platform-Digital-Prison-Services-fred",
    "Message" : "{\"eventId\":\"123456\",\"eventType\":\"ALERT\",\"eventDatetime\":\"2019-12-06T13:49:30.725568\",\"rootOffenderId\":234567,\"offenderIdDisplay\":\"AB1234D\",\"agencyLocationId\":\"LPI\"}",
    "Timestamp" : "2019-12-06T13:50:10.638Z",
    "MessageAttributes" : {
      "eventType" : {"Type":"String","Value":"ALERT"},
      "id" : {"Type":"String","Value":"495b34d5-d0ae-d698-3c38-fce3e15c6918"},
      "contentType" : {"Type":"String","Value":"text/plain;charset=UTF-8"},
      "timestamp" : {"Type":"Number.java.lang.Long","Value":"1575640210634"}
    }
  }
    """.trimIndent()

  @Test
  fun `case note service called with hydrated event`() {
    whenever(caseNotesService.getCaseNote(anyString(), anyString())).thenReturn(createCaseNote())
    pusher.pushCaseNoteToDelius(validCaseNoteEvent)
    verify(caseNotesService).getCaseNote("G4803UT", "1234")
  }

  @Test
  fun `case note service calls telemetry client`() {
    whenever(caseNotesService.getCaseNote(anyString(), anyString())).thenReturn(createCaseNote())
    pusher.pushCaseNoteToDelius(validCaseNoteEvent)
    verify(telemetryClient).trackEvent(
      eq("CaseNoteCreate"),
      check {
        assertThat(it).containsExactlyInAnyOrderEntriesOf(
          mapOf("caseNoteId" to "1234", "type" to "NEG-IEP_WARN", "eventId" to "123456")
        )
      },
      isNull()
    )
  }

  @Test
  fun `delius service called with case note from case notes service`() {
    val caseNote = createCaseNote()
    whenever(caseNotesService.getCaseNote(anyString(), anyString())).thenReturn(caseNote)
    pusher.pushCaseNoteToDelius(validCaseNoteEvent)
    verify(communityApiService).postCaseNote(createDeliusCaseNote(caseNote))
  }

  @Test
  fun `case note service not called with hydrated invalid event`() {
    pusher.pushCaseNoteToDelius(invalidCaseNoteEvent)
    verify(caseNotesService, never()).getCaseNote(anyString(), anyString())
  }

  @Test
  fun `delius service not called if case note has empty text`() {
    whenever(caseNotesService.getCaseNote(anyString(), anyString())).thenReturn(createCaseNote(text = ""))
    pusher.pushCaseNoteToDelius(validCaseNoteEvent)
    verify(communityApiService, never()).postCaseNote(any())
  }

  private fun createCaseNote(text: String = "note content") = CaseNote(
    eventId = 123456,
    offenderIdentifier = "offenderId",
    type = "NEG",
    subType = "IEP_WARN",
    creationDateTime = OffsetDateTime.parse("2019-04-16T11:22:33+00:00"),
    occurrenceDateTime = OffsetDateTime.parse("2019-03-23T11:22:00+00:00"),
    authorName = "Some Name",
    text = text,
    locationId = "LEI",
    amendments = listOf()
  )

  private fun createDeliusCaseNote(caseNote: CaseNote) = DeliusCaseNote(
    header = CaseNoteHeader("offenderId", 123456),
    body = CaseNoteBody(
      noteType = "${caseNote.type} ${caseNote.subType}",
      content = caseNote.text,
      contactTimeStamp = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(caseNote.occurrenceDateTime),
      systemTimeStamp = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(caseNote.creationDateTime),
      staffName = caseNote.getAuthorNameWithComma(),
      establishmentCode = caseNote.locationId!!
    )
  )
}
