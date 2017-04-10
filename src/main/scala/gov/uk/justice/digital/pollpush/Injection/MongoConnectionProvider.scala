package gov.uk.justice.digital.pollpush.Injection

import com.google.inject.name.Named
import com.google.inject.{Inject, Provider}
import reactivemongo.api.{MongoConnection, MongoDriver}

class MongoConnectionProvider @Inject() (driver: MongoDriver, @Named("mongoUri") mongoUri: String) extends Provider[MongoConnection] {

  override def get() = driver.connection(MongoConnection.parseURI(mongoUri).get)
}
