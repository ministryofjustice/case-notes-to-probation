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
    val (Message) = gson.fromJson<Message>(requestJson, Message::class.java)
    val (offenderIdDisplay, caseNoteId) = gson.fromJson<CaseNoteMessage>(Message, CaseNoteMessage::class.java)

    val caseNote = caseNotesService.getCaseNote(offenderIdDisplay, caseNoteId)
    log.debug("Found case note {} in case notes service, now pushing to delius", caseNoteId)
    deliusService.postCaseNote(DeliusCaseNote(caseNote))
  }
}

data class Message(val Message: String)
data class CaseNoteMessage(val offenderIdDisplay: String, val caseNoteId: String)
