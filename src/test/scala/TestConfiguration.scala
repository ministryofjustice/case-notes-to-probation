import gov.uk.justice.digital.pollpush.Configuration
import gov.uk.justice.digital.pollpush.traits.{BulkSource, SingleTarget}

class TestConfiguration(bulkSource: BulkSource, singleTarget: SingleTarget) extends Configuration {

  override protected def configureOverridable() {

    bind[BulkSource].toInstance(bulkSource)
    bind[SingleTarget].toInstance(singleTarget)
  }
}
