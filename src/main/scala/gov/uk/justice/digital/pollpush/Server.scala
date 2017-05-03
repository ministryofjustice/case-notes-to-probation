package gov.uk.justice.digital.pollpush

import akka.actor.{Actor, ActorSystem, Props}
import ch.qos.logback.classic.Level
import com.google.inject.name.Names
import com.google.inject.{Guice, Module}
import gov.uk.justice.digital.pollpush.actor.{Poller, Pusher}
import grizzled.slf4j.{Logger, Logging}
import net.codingwell.scalaguice.InjectorExtensions._

import scala.reflect.ClassTag

object Server extends App with Logging {

  logger.info("Started PollPush Service ...")

  def run(config: Module = new Configuration) = {

    val injector = Guice.createInjector(config)
    val system = injector.instance[ActorSystem]

    (injector.instance[Boolean](Names.named("debugLog")), Logger.rootLogger.logger) match { // DEBUG_LOG=true

      case (true, rootLogger: ch.qos.logback.classic.Logger) => rootLogger.setLevel(Level.DEBUG) // Set Logback to DEBUG if required
      case _ =>
    }

    def startActor[T <: Actor: Manifest] =
      system.actorOf(Props(injector.instance[T]), implicitly[ClassTag[T]].runtimeClass.getName.split('.').last)

    startActor[Pusher]
    startActor[Poller]

    system
  }

  run()
}
