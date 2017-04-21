package Helpers

import gov.uk.justice.digital.pollpush.data._

object SourceCaseNoteBuilder {

  def build(nomisId: String, noteId: String, noteType: String, content: String, timestamp: String, staffName: String, establishmentCode: String) =

    SourceCaseNote(
      nomisId,
      noteId.toInt,
      content,
      timestamp,
      noteType,
      staffName,
      establishmentCode
    )
}
