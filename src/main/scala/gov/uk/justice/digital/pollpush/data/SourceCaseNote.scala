package gov.uk.justice.digital.pollpush.data

case class SourceCaseNote(nomisId: String, noteId: String, noteType: String, content: String, timestamp: String) {

  def toTarget = TargetCaseNote(TargetCaseNoteHeader(nomisId, noteId), TargetCaseNoteBody(noteType, content, timestamp))
}
