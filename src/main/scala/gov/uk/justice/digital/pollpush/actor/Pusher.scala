package gov.uk.justice.digital.pollpush.actor

import akka.actor.{Actor, ActorLogging}
import akka.pattern.pipe
import com.google.inject.Inject
import gov.uk.justice.digital.pollpush.data._
import gov.uk.justice.digital.pollpush.traits.{DataStore, SingleTarget}

import scala.concurrent.ExecutionContext.Implicits.global

class Pusher @Inject() (target: SingleTarget, store: DataStore) extends Actor with ActorLogging {

  override def receive = {

    case caseNote @ TargetCaseNote(header, _, _) =>

      log.info(s"Caching Case Note: $header ...")
      store.save(caseNote).pipeTo(self)

    case storeResult @ SaveResult(caseNote, _) =>

      for (error <- storeResult.error) log.warning(s"${caseNote.header} SAVE ERROR: ${error.getMessage}")

      log.info(s"Pushing Case Note: ${caseNote.header} ...")
      target.push(caseNote).pipeTo(self)

    case pushResult @ PushResult(caseNote, _, body, _) =>

      (pushResult.result, pushResult.error) match {

        case (_, Some(error)) => log.warning(s"${caseNote.header} PUSH ERROR: ${error.getMessage}")
        case (Some(result), None) => log.info(s"${caseNote.header} ${result.value} $body")
        case _ => log.warning("PUSH ERROR: No result or error")
      }

      log.info(s"Purging Case Note: ${caseNote.header} ...")
      store.delete(caseNote).pipeTo(self)

    case deleteResult @ DeleteResult(caseNote, _) =>

      for (error <- deleteResult.error) log.warning(s"${caseNote.header} DELETE ERROR: ${error.getMessage}")

      store.count.pipeTo(self)

    case countResult @ CountResult(total, _) =>

      for (error <- countResult.error) log.warning(s"COUNT ERROR: ${error.getMessage}")

      if (total == 0) store.pullProcessed().pipeTo(self) // Pipes processed complete to EmptyResult below

    case EmptyResult(Some(error)) => log.warning(s"PROCESSED ERROR: ${error.getMessage}")

    case EmptyResult(None) => log.info("Case Notes pull set successfully processed")
  }
}
