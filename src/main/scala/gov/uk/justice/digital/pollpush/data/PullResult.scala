package gov.uk.justice.digital.pollpush.data

import akka.http.scaladsl.model.DateTime

case class PullResult(casenotes: Seq[SourceCaseNote], from: Option[DateTime], until: Option[DateTime], error: Option[Throwable])
