package gov.uk.justice.digital.pollpush

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.google.inject.AbstractModule
import gov.uk.justice.digital.pollpush.Injection._
import gov.uk.justice.digital.pollpush.services.{DeliusTarget, JwtTokenGenerator, MongoStore, NomisSource}
import gov.uk.justice.digital.pollpush.traits.{BulkSource, DataStore, SingleTarget, SourceToken}
import net.codingwell.scalaguice.ScalaModule
import org.json4s.Formats
import reactivemongo.api.{MongoConnection, MongoDriver}
import scala.util.Properties

class Configuration extends AbstractModule with ScalaModule {

  private def envOrDefault(key: String) = Properties.envOrElse(key, envDefaults(key))

  private def bindNamedValue[T: Manifest](name: String, value: T) = bind[T].annotatedWithName(name).toInstance(value)

  private def bindConfiguration[T: Manifest](map: Map[String, String], transform: String => T) =

    for ((name, value) <- map.mapValues(envOrDefault).mapValues(transform)) bindNamedValue(name, value)


  protected def envDefaults = Map(
    "DEBUG_LOG" -> "false",
    "MONGO_DB_URL" -> "mongodb://localhost:27017",
    "MONGO_DB_NAME" -> "pollpush",
    "PULL_BASE_URL" -> "http://localhost:8080/nomisapi/offenders/events/case_notes",
    "PULL_NOTE_TYPES" -> "",
    "PUSH_BASE_URL" -> "http://localhost:8080/delius", // ?from=
    "PUSH_USERNAME" -> "username",
    "PUSH_PASSWORD" -> "password",
    "POLL_SECONDS" -> "60",
    "NOMIS_TOKEN" -> "abcde12345",
    "PRIVATE_KEY" -> "MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQg0I/O+oZH/OAQVJHB8dvAD7gBMRUtwsFW75y7p1aflKSgCgYIKoZIzj0DAQehRANCAASAgCGtL4MyJc5xGfgIY/UP6EfHH09MsvOWeWL3tyod0QVia1yTrTQycjUc9sbgHZGxQJOi2fI6CBWZNBZ/MZso",
    "PUBLIC_KEY" -> "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEgIAhrS+DMiXOcRn4CGP1D+hHxx9PTLLzlnli97cqHdEFYmtck600MnI1HPbG4B2RsUCTotnyOggVmTQWfzGbKA=="
  )

  override final def configure() {

    bindConfiguration(
      Map(
        "mongoUri" -> "MONGO_DB_URL",
        "dbName" -> "MONGO_DB_NAME",
        "sourceUrl" -> "PULL_BASE_URL",
        "targetUrl" -> "PUSH_BASE_URL",
        "username" -> "PUSH_USERNAME",
        "password" -> "PUSH_PASSWORD",
        "nomisToken" -> "NOMIS_TOKEN",
        "privateKey" -> "PRIVATE_KEY"
      ),
      identity
    )

    bindConfiguration(
      Map(
        "timeout" -> "POLL_SECONDS"
      ),
      s => s.toInt
    )

    bindConfiguration(
      Map(
        "debugLog" -> "DEBUG_LOG"
      ),
      s => s.toBoolean
    )

    bindConfiguration(
      Map(
        "noteTypes" -> "PULL_NOTE_TYPES"
      ),
      s => s.split(',').toSeq
    )

    bind[Formats].toProvider[FormatsProvider]
    bind[MongoConnection].toProvider[MongoConnectionProvider]
    bind[ActorMaterializer].toProvider[ActorMaterializerProvider]

    bind[ActorSystem].toProvider[ActorSystemProvider].asEagerSingleton()
    bind[MongoDriver].toProvider[MongoDriverProvider].asEagerSingleton()

    bind[SourceToken].to[JwtTokenGenerator]

    configureOverridable()
  }

  protected def configureOverridable() {

    bind[DataStore].to[MongoStore]
    bind[BulkSource].to[NomisSource]
    bind[SingleTarget].to[DeliusTarget]
  }
}
