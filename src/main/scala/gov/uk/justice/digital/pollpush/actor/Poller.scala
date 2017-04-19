package gov.uk.justice.digital.pollpush.actor

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.model.DateTime
import akka.pattern.pipe
import com.google.inject.Inject
import com.google.inject.name.Named
import gov.uk.justice.digital.pollpush.data._
import gov.uk.justice.digital.pollpush.traits.{BulkSource, DataStore}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class Poller @Inject() (source: BulkSource, store: DataStore, @Named("timeout") timeout: Int) extends Actor with ActorLogging {

  log.info(s"Poller created to pull every $timeout seconds ...")

  private case class PullRequest(from: DateTime)

  private val duration = timeout.seconds

  private def pusher = context.actorSelection("/user/Pusher")

  override def preStart = store.allCaseNotes.pipeTo(self)

  override def receive = {

    case recoverResult @ DataResult(caseNotes, _) =>

      for (error <- recoverResult.error) log.warning(s"RECOVER ERROR:${error.getMessage}")

      if (caseNotes.nonEmpty) {

        log.info(s"Recovered ${caseNotes.length} Case Note(s) ...")

        caseNotes.foreach { caseNote => pusher ! SaveResult(caseNote, None) } // Push recovered results, wait for timeout before pulling again as may complete recovery first

        store.lastProcessedPull.foreach(context.system.scheduler.scheduleOnce(duration, self, _))

      } else {  // Nothing to recover, so start pulling from last processed date

        store.lastProcessedPull.pipeTo(self)
      }

    case lastResult @ LastResult(from, _)  =>

      for (error <- lastResult.error) log.warning(s"LAST ERROR: ${error.getMessage}")

      self ! PullRequest(from.getOrElse(DateTime.now))

    case PullRequest(from) =>

      log.info(s"Pulling Case Notes from $from ...")
      source.pull(from, DateTime.now).pipeTo(self)

    case pullResult @ PullResult(caseNotes, Some(from), Some(until), _) =>

      context.system.scheduler.scheduleOnce(duration, self, PullRequest(pullResult.error match {

        case Some(error) =>

          log.warning(s"PULL ERROR: ${error.getMessage}")
          from

        case None =>

          log.info(s"Pulled ${caseNotes.length} Case Note(s) from $from until $until")

          if (caseNotes.nonEmpty) { // pullProcessed() called after all caseNotes pushed and purged in Pusher for nonEmpty caseNotes

            store.pullReceived(until).foreach {

              case EmptyResult(Some(error)) => log.warning(s"RECEIVED ERROR: ${error.getMessage}")
              case EmptyResult(None) =>
            }

            for (caseNote <- caseNotes) pusher ! caseNote.toTarget

          } else {

            store.pullReceived(until).pipeTo(self) // Pipes received complete to EmptyResult below when no caseNotes
          }
          until
      }))

    case EmptyResult(Some(error)) => log.warning(s"EMPTY RECEIVED ERROR: ${error.getMessage}")

    case EmptyResult(None) => store.pullProcessed().foreach {

      case EmptyResult(Some(error)) => log.warning(s"EMPTY PROCESSED ERROR: ${error.getMessage}")

      case EmptyResult(None) => log.info("Empty Case Notes pull set processed")
    }
  }
}
