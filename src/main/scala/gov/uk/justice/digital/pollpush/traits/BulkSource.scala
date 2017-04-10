package gov.uk.justice.digital.pollpush.traits

import akka.http.scaladsl.model.DateTime
import gov.uk.justice.digital.pollpush.data.PullResult
import scala.concurrent.Future

trait BulkSource {

  def pull(from: DateTime, until: DateTime): Future[PullResult]
}
