package gov.uk.justice.digital.pollpush.data

case class PullResult(casenotes: Seq[SourceCaseNote], error: Option[Throwable])
