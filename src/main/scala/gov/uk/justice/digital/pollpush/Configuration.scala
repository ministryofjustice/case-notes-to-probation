package gov.uk.justice.digital.pollpush

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.google.inject.AbstractModule
import gov.uk.justice.digital.pollpush.Injection._
import gov.uk.justice.digital.pollpush.services.{DeliusTarget, MongoStore, NomisSource}
import gov.uk.justice.digital.pollpush.traits.{BulkSource, DataStore, SingleTarget}
import net.codingwell.scalaguice.ScalaModule
import org.json4s.Formats
import reactivemongo.api.{MongoConnection, MongoDriver}

import scala.util.Properties

class Configuration extends AbstractModule with ScalaModule {

  private def envOrDefault(key: String) = Properties.envOrElse(key, envDefaults(key))

  protected def envDefaults = Map(
    "MONGO_DB_URL" -> "mongodb://localhost:27017",
    "MONGO_DB_NAME" -> "pollpush",
    "PULL_BASE_URL" -> "http://localhost:8080/nomis/casenotes",
    "PUSH_BASE_URL" -> "http://localhost:8080/delius", // ?from=
    "PUSH_USERNAME" -> "username",
    "PUSH_PASSWORD" -> "password",
    "POLL_SECONDS" -> "60"
  )

  override final def configure() {

    val textMaps = Map(
      "mongoUri" -> "MONGO_DB_URL",
      "dbName" -> "MONGO_DB_NAME",
      "sourceUrl" -> "PULL_BASE_URL",
      "targetUrl" -> "PUSH_BASE_URL",
      "username" -> "PUSH_USERNAME",
      "password" -> "PUSH_PASSWORD").mapValues(envOrDefault)

    val numberMaps = Map(
      "timeout" -> "POLL_SECONDS").mapValues(envOrDefault(_).toInt)

    for ((name, text) <- textMaps) bind[String].annotatedWithName(name).toInstance(text)
    for ((name, number) <- numberMaps) bind[Int].annotatedWithName(name).toInstance(number)

    bind[Formats].toProvider[FormatsProvider]
    bind[MongoConnection].toProvider[MongoConnectionProvider]
    bind[ActorMaterializer].toProvider[ActorMaterializerProvider]

    bind[ActorSystem].toProvider[ActorSystemProvider].asEagerSingleton()
    bind[MongoDriver].toProvider[MongoDriverProvider].asEagerSingleton()

    configureOverridable()
  }

  protected def configureOverridable() {

    bind[DataStore].to[MongoStore]
    bind[BulkSource].to[NomisSource]
    bind[SingleTarget].to[DeliusTarget]
  }
}
