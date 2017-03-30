package gov.uk.justice.digital.pollpush.traits

import gov.uk.justice.digital.pollpush.data.PullResult
import scala.concurrent.Future

trait BulkSource {

  def pull(): Future[PullResult]
}
