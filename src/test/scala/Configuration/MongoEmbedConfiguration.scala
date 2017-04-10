package Configuration

import gov.uk.justice.digital.pollpush.Configuration

class MongoEmbedConfiguration (mongoEmbedPort: Int = 12345) extends Configuration {

  override def envDefaults = super.envDefaults + ("MONGO_DB_URL" -> s"mongodb://localhost:$mongoEmbedPort")
}
