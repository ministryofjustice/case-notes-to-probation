package gov.uk.justice.digital.pollpush

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.google.inject.AbstractModule
import gov.uk.justice.digital.pollpush.Injection._
import gov.uk.justice.digital.pollpush.services.{DeliusTarget, NomisSource}
import gov.uk.justice.digital.pollpush.traits.{BulkSource, SingleTarget}
import net.codingwell.scalaguice.ScalaModule
import org.json4s.Formats
import scala.util.Properties

class Configuration extends AbstractModule with ScalaModule {

  override final def configure() {

    val textMaps = Map(
      "sourceUrl" -> Properties.envOrElse("PULL_BASE_URL", "http://localhost:8080/nomis/casenotes"),
      "targetUrl" -> Properties.envOrElse("PUSH_BASE_URL", "http://localhost:8080/delius"),
      "username" -> Properties.envOrElse("PUSH_USERNAME", "username"),
      "password" -> Properties.envOrElse("PUSH_PASSWORD", "password")
    )

    val numberMaps = Map(
      "seconds" -> Properties.envOrElse("POLL_SECONDS", "15").toInt  //@TODO: Read from resource file?
    )

    for ((name, text) <- textMaps) bind[String].annotatedWithName(name).toInstance(text)
    for ((name, number) <- numberMaps) bind[Int].annotatedWithName(name).toInstance(number)

    bind[Formats].toProvider[FormatsProvider]
    bind[ActorMaterializer].toProvider[ActorMaterializerProvider]
    bind[ActorSystem].toProvider[ActorSystemProvider].asEagerSingleton()

    configureOverridable()
  }

  protected def configureOverridable() {

    bind[BulkSource].to[NomisSource]
    bind[SingleTarget].to[DeliusTarget]
  }
}
