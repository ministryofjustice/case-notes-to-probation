package gov.uk.justice.digital.pollpush.actor

import akka.actor.Actor
import akka.pattern.pipe
import com.google.inject.Inject
import grizzled.slf4j.Logging
import gov.uk.justice.digital.pollpush.data.{PushResult, TargetCaseNote}
import gov.uk.justice.digital.pollpush.traits.SingleTarget
import scala.concurrent.ExecutionContext.Implicits.global

class Pusher @Inject() (target: SingleTarget) extends Actor with Logging {

  override def receive = {

    case caseNote @ TargetCaseNote(header, _) =>

      logger.info(s"Pushing Case Note: $header ...")
      target.push(caseNote).pipeTo(self)

    case PushResult(caseNote, _, _, Some(error)) => logger.warn(s"${caseNote.header} ERROR", error)

    case PushResult(caseNote, Some(status), body, None) => logger.info(s"${caseNote.header} ${status.value} $body")
  }
}
