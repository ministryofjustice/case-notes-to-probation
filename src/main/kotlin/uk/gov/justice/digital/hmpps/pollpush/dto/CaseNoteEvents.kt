package uk.gov.justice.digital.hmpps.pollpush.dto

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDateTime


@JsonInclude(JsonInclude.Include.NON_NULL)
data class CaseNoteEvents(val events: List<CaseNoteEvent>, val latestEventDate: LocalDateTime)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CaseNoteEvent(val nomsId: String,
                         val id: String,
                         val content: String,
                         val contactTimestamp: LocalDateTime,
                         val notificationTimestamp: LocalDateTime,
                         val staffName: String,
                         val establishmentCode: String,
                         val noteType: String)
