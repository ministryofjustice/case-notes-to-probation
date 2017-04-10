package Helpers

import Configuration.MongoEmbedConfiguration
import com.google.inject.Guice
import gov.uk.justice.digital.pollpush.traits.DataStore
import grizzled.slf4j.Logging
import net.codingwell.scalaguice.InjectorExtensions._

object MongoEmbedClient extends Logging {

  def store(mongoEmbedPort: Int = 12345) = Guice.createInjector(new MongoEmbedConfiguration(mongoEmbedPort)).instance[DataStore]
}
