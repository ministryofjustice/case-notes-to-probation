package uk.gov.justice.digital.hmpps.pollpush.utils

import org.slf4j.MDC
import org.springframework.stereotype.Component

@Component
object MdcUtility {
  val REQUEST_DURATION = "duration"
  val RESPONSE_STATUS = "status"
  val SKIP_LOGGING = "skipLogging"

  val isLoggingAllowed: Boolean
    get() = "true" != MDC.get(SKIP_LOGGING)
}
