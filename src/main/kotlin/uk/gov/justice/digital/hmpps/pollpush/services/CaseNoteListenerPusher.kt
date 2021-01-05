package uk.gov.justice.digital.hmpps.pollpush.services

import com.google.gson.Gson
import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Service

@Service
class CaseNoteListenerPusher(
  private val caseNotesService: CaseNotesService,
  private val deliusService: DeliusService,
  private val telemetryClient: TelemetryClient,
  private val gson: Gson
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @JmsListener(destination = "\${sqs.queue.name}")
  fun pushCaseNoteToDelius(requestJson: String?) {
    val (Message, MessageId) = gson.fromJson<Message>(requestJson, Message::class.java)
    val (offenderIdDisplay, caseNoteId, eventType) = gson.fromJson(Message, CaseNoteMessage::class.java)

    if (caseNoteId.isNullOrEmpty()) {
      log.warn("Ignoring null case note id for message with id {} and type {}", MessageId, eventType)
      return
    }

    val caseNote = caseNotesService.getCaseNote(offenderIdDisplay, caseNoteId)
    if (caseNote.isInvalid(MessageId, eventType)) {
      return
    }

    with(caseNote!!) {
      log.debug(
        "Found case note {} of type {} {} in case notes service, now pushing to delius with event id {}",
        caseNoteId,
        type,
        subType,
        eventId
      )
      telemetryClient.trackEvent(
        "CaseNoteCreate",
        mapOf("caseNoteId" to caseNoteId, "type" to "$type-$subType", "eventId" to eventId.toString()),
        null
      )
    }
    deliusService.postCaseNote(DeliusCaseNote(caseNote))
  }

  private fun CaseNote?.isInvalid(messageId: String, eventType: String): Boolean {
    if (this == null) {
      log.warn(
        "Ignoring case note id for message with id {} and type {} because we could not find the case note",
        messageId,
        eventType
      )
      return true
    }

    if (this.text.isEmpty()) {
      log.warn(
        "Ignoring case note id for message with id {} and type {} because case note text is empty",
        messageId,
        eventType
      )
      return true
    }

    return false
  }
}

data class Message(val Message: String, val MessageId: String)
data class CaseNoteMessage(val offenderIdDisplay: String, val caseNoteId: String?, val eventType: String)
