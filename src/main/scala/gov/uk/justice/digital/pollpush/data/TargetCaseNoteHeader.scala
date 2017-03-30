package gov.uk.justice.digital.pollpush.data

case class TargetCaseNoteHeader(nomisId: String, noteId: String) {

  override def toString = s"$nomisId/$noteId"
}
