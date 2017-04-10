package gov.uk.justice.digital.pollpush.data

case class DeleteResult(caseNote: TargetCaseNote, error: Option[Throwable])
