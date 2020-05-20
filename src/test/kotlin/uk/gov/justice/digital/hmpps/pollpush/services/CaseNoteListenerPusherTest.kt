package uk.gov.justice.digital.hmpps.pollpush.services

import com.google.gson.GsonBuilder
import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import java.time.LocalDateTime

class CaseNoteListenerPusherTest {
  private val caseNotesService: CaseNotesService = mock()
  private val deliusService: DeliusService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val gson = GsonBuilder().create()

  private val pusher = CaseNoteListenerPusher(caseNotesService, deliusService, telemetryClient, gson)

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

  private val invalidCaseNoteEvent = """{
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
  }""".trimIndent()

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
    verify(telemetryClient).trackEvent(eq("CaseNoteCreate"), check {
      assertThat(it).containsExactlyInAnyOrderEntriesOf(
          mapOf("caseNoteId" to "1234", "type" to "NEG-IEP_WARN", "eventId" to "123456"))
    }, isNull())
  }

  @Test
  fun `delius service called with case note from case notes service`() {
    whenever(caseNotesService.getCaseNote(anyString(), anyString())).thenReturn(createCaseNote())
    pusher.pushCaseNoteToDelius(validCaseNoteEvent)
    verify(deliusService).postCaseNote(createDeliusCaseNote())
  }

  @Test
  fun `case note service not called with hydrated invalid event`() {
    pusher.pushCaseNoteToDelius(invalidCaseNoteEvent)
    verify(caseNotesService, never()).getCaseNote(anyString(), anyString())
  }

  private fun createCaseNote() = CaseNote(
      eventId = 123456,
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
      header = CaseNoteHeader("offenderId", 123456),
      body = CaseNoteBody(
          noteType = "NEG IEP_WARN",
          content = "note content",
          contactTimeStamp = "2019-03-23T11:22:00.000Z",
          systemTimeStamp = "2019-04-16T11:22:33.000Z",
          staffName = "Name, Some",
          establishmentCode = "LEI"))
}
