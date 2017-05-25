package Helpers

import gov.uk.justice.digital.pollpush.data._

object SourceCaseNoteBuilder {

  def build(nomisId: String, noteId: String, noteType: String, content: String, contactTimestamp: String, raisedTimestamp: String, staffName: String, establishmentCode: String) =

    SourceCaseNote(
      nomisId,
      noteId.toInt,
      content,
      contactTimestamp,
      raisedTimestamp,
      noteType,
      staffName,
      establishmentCode
    )
}
