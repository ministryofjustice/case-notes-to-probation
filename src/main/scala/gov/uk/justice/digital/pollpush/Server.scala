package gov.uk.justice.digital.pollpush

import akka.actor.{ActorSystem, Props}
import com.google.inject.{Guice, Module}
import gov.uk.justice.digital.pollpush.actor.{Poller, Pusher}
import grizzled.slf4j.Logging
import net.codingwell.scalaguice.InjectorExtensions._

object Server extends App with Logging {

  logger.info("Started PollPush Service ...")

  def run(config: Module = new Configuration) = {

    val injector = Guice.createInjector(config)
    val system = injector.instance[ActorSystem]

    system.actorOf(Props(injector.instance[Pusher]), "Pusher")
    system.actorOf(Props(injector.instance[Poller]), "Poller")

    system
  }

  run()
}
