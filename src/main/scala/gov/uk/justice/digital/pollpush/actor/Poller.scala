package gov.uk.justice.digital.pollpush.actor

import akka.actor.Actor
import akka.pattern.pipe
import com.google.inject.Inject
import com.google.inject.name.Named
import gov.uk.justice.digital.pollpush.data.PullResult
import gov.uk.justice.digital.pollpush.traits.BulkSource
import grizzled.slf4j.Logging
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

class Poller @Inject() (source: BulkSource, @Named("seconds") seconds: Int) extends Actor with Logging { // TODO: ActorLogging ?

  logger.info(s"Poller created to tick every $seconds seconds ...")

  private case object Tick

  override def preStart = context.system.scheduler.schedule(Duration.Zero, Duration.create(seconds, TimeUnit.SECONDS), self, Tick)

  override def receive = {

    case Tick =>

      logger.info("Pulling Case Notes ...")
      source.pull().pipeTo(self)

    case PullResult(_, Some(error)) => logger.warn("ERROR", error)

    case PullResult(caseNotes, None) =>

      logger.info(s"Found ${caseNotes.length} Case Note(s)")

      val pusher = context.actorSelection("/user/Pusher") // Could send to a round-robin router to scale up if needed
      for (caseNote <- caseNotes) pusher ! caseNote.toTarget
  }
}
