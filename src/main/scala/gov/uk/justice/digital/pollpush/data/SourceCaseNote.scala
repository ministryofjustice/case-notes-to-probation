package gov.uk.justice.digital.pollpush.data

case class SourceCaseNote(nomisId: String, noteId: String, noteType: String, content: String, timestamp: String, staffName: String, establishmentCode: String) {

  def toTarget = TargetCaseNote(TargetCaseNoteHeader(nomisId, noteId), TargetCaseNoteBody(noteType, content, timestamp, staffName, establishmentCode))
}
