package gov.uk.justice.digital.pollpush.data

case class SaveResult(caseNote: TargetCaseNote, error: Option[Throwable])
