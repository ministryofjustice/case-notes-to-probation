package Helpers

import gov.uk.justice.digital.pollpush.data._

object SourceCaseNoteBuilder {

  def build(nomisId: String, noteId: String, noteType: String, content: String, timestamp: String, staffName: String, establishmentCode: String) =

    SourceCaseNote(
      nomisId,
      establishmentCode,
      SourceCaseNoteOuter(
        SourceCaseNoteInner(
          noteId,
          timestamp,
          content,
          SourceCaseNoteType(
            noteType
          ),
          SourceCaseNoteStaff(
            staffName
          )
        )
      )
    )
}
