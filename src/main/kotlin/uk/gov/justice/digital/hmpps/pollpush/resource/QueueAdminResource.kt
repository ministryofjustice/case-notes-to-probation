package uk.gov.justice.digital.hmpps.pollpush.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.pollpush.services.QueueAdminService

@RestController
@RequestMapping("/queue-admin", produces = [MediaType.APPLICATION_JSON_VALUE])
class QueueAdminResource(private val queueAdminService: QueueAdminService) {

  @PutMapping("/purge-dlq")
  @PreAuthorize("hasRole('CASE_NOTE_QUEUE_ADMIN')")
  @Operation(
    summary = "Purges the DLQ",
    description = "Requires ROLE_CASE_NOTE_QUEUE_ADMIN"
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an authorisation with ROLE_CASE_NOTE_QUEUE_ADMIN"
      )
    ]
  )
  fun purgeDlq(): Unit = queueAdminService.clearAllDlqMessages()

  @PutMapping("/transfer-dlq")
  @PreAuthorize("hasRole('CASE_NOTE_QUEUE_ADMIN')")
  @Operation(
    summary = "Transfers all DLQ messages to the main queue",
    description = "Requires ROLE_CASE_NOTE_QUEUE_ADMIN"
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an authorisation with ROLE_CASE_NOTE_QUEUE_ADMIN"
      )
    ]
  )
  fun transferDlq(): Unit = queueAdminService.transferDlqMessages()

  @PutMapping("/queue-housekeeping")
  @Operation(
    summary = "Triggers maintenance of the queues",
    description = "This is an internal service which isn't exposed to the outside world. It is called from a Kubernetes CronJob named `queue-housekeeping-cronjob`"
  )
  fun eventQueueHousekeeping() = queueAdminService.transferDlqMessages()
}
