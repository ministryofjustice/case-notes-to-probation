package gov.uk.justice.digital.pollpush.data

case class DataResult(casenotes: Seq[TargetCaseNote], error: Option[Throwable])
