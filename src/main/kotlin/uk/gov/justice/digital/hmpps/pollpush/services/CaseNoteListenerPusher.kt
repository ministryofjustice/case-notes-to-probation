package uk.gov.justice.digital.hmpps.pollpush.services

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Service


@Service
open class CaseNoteListenerPusher(private val caseNotesService: CaseNotesService,
                                  private val deliusService: DeliusService) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
    val gson: Gson = GsonBuilder().create()
  }

  @JmsListener(destination = "\${sqs.queue.name}")
  open fun pushCaseNoteToDelius(requestJson: String?) {
    val (Message, MessageId) = gson.fromJson<Message>(requestJson, Message::class.java)
    val (offenderIdDisplay, caseNoteId, eventType) = gson.fromJson<CaseNoteMessage>(Message, CaseNoteMessage::class.java)

    if (caseNoteId.isNullOrEmpty()) {
      log.warn("Ignoring null case note id for message with id {} and type {}", MessageId, eventType)
    } else {
      val caseNote = caseNotesService.getCaseNote(offenderIdDisplay, caseNoteId)
      log.debug("Found case note {} of type {} {} in case notes service, now pushing to delius with event id {}", caseNoteId, caseNote.type, caseNote.subType, caseNote.eventId)
      deliusService.postCaseNote(DeliusCaseNote(caseNote))
    }
  }
}

data class Message(val Message: String, val MessageId: String)
data class CaseNoteMessage(val offenderIdDisplay: String, val caseNoteId: String?, val eventType: String)
