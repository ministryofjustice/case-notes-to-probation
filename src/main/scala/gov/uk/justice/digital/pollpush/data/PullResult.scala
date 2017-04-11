package gov.uk.justice.digital.pollpush.data

import akka.http.scaladsl.model.DateTime

case class PullResult(events: Seq[SourceCaseNote], from: Option[DateTime], until: Option[DateTime], error: Option[Throwable])
