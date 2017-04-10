package gov.uk.justice.digital.pollpush.traits

import akka.http.scaladsl.model.DateTime
import gov.uk.justice.digital.pollpush.data._
import scala.concurrent.Future

trait DataStore {

  def save(caseNote: TargetCaseNote): Future[SaveResult]
  def delete(caseNote: TargetCaseNote): Future[DeleteResult]

  def count: Future[CountResult]
  def allCaseNotes: Future[DataResult]

  def pullReceived(dateTime: DateTime): Future[EmptyResult]
  def pullProcessed(): Future[EmptyResult]

  def lastProcessedPull: Future[LastResult]
}
