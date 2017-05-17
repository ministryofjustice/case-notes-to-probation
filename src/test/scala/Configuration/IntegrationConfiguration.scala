package Configuration

class IntegrationConfiguration(pullNoteTypes: String) extends MongoEmbedConfiguration {

  override def envDefaults = super.envDefaults + ("PULL_NOTE_TYPES" -> pullNoteTypes)
}
