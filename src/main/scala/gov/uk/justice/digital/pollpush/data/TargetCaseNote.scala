package gov.uk.justice.digital.pollpush.data

case class TargetCaseNote(header: TargetCaseNoteHeader, body: TargetCaseNoteBody, id: Option[String] = None)
