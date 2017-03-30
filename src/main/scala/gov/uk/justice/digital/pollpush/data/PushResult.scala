package gov.uk.justice.digital.pollpush.data

import akka.http.scaladsl.model.StatusCode

case class PushResult(caseNote: TargetCaseNote, result: Option[StatusCode], body: String, error: Option[Throwable])
