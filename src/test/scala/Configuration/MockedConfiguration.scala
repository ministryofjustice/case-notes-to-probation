package Configuration

import gov.uk.justice.digital.pollpush.Configuration
import gov.uk.justice.digital.pollpush.traits.{BulkSource, DataStore, SingleTarget}

class MockedConfiguration(bulkSource: BulkSource, singleTarget: SingleTarget, dataStore: DataStore, timeout: Int) extends Configuration {

  override def envDefaults = super.envDefaults + ("POLL_SECONDS" -> timeout.toString)

  override protected def configureOverridable() {

    bind[DataStore].toInstance(dataStore)
    bind[BulkSource].toInstance(bulkSource)
    bind[SingleTarget].toInstance(singleTarget)
  }
}
