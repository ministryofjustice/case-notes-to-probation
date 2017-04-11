package gov.uk.justice.digital.pollpush.data

case class SourceCaseNoteType(code: String)

case class SourceCaseNoteStaff(name: String)

case class SourceCaseNoteInner(id: String, contact_datetime: String, text: String, `type`: SourceCaseNoteType, staff_member: SourceCaseNoteStaff)

case class SourceCaseNoteOuter(case_note: SourceCaseNoteInner)

case class SourceCaseNote(noms_id: String, prison_id: String, case_note: SourceCaseNoteOuter) {

  def toTarget = TargetCaseNote(
    TargetCaseNoteHeader(noms_id, case_note.case_note.id),
    TargetCaseNoteBody(
      case_note.case_note.`type`.code,
      case_note.case_note.text,
      case_note.case_note.contact_datetime,
      case_note.case_note.staff_member.name,
      prison_id
    )
  )
}
