package gov.uk.justice.digital.pollpush.traits

import gov.uk.justice.digital.pollpush.data.{PushResult, TargetCaseNote}
import scala.concurrent.Future

trait SingleTarget {

  def push(caseNote: TargetCaseNote): Future[PushResult]
}
