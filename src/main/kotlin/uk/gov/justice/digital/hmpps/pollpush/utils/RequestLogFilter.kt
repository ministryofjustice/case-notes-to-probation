package uk.gov.justice.digital.hmpps.pollpush.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.Order
import org.springframework.lang.NonNull
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import uk.gov.justice.digital.hmpps.pollpush.utils.MdcUtility.REQUEST_DURATION
import uk.gov.justice.digital.hmpps.pollpush.utils.MdcUtility.RESPONSE_STATUS
import uk.gov.justice.digital.hmpps.pollpush.utils.MdcUtility.SKIP_LOGGING
import uk.gov.justice.digital.hmpps.pollpush.utils.MdcUtility.isLoggingAllowed
import java.io.IOException
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
@Order(3)
class RequestLogFilter @Autowired
constructor(@Value("\${logging.uris.exclude.regex}") excludeUris: String) : OncePerRequestFilter() {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS")

  private val excludeUriRegex: Pattern

  init {
    excludeUriRegex = Pattern.compile(excludeUris)
  }

  @Throws(ServletException::class, IOException::class)
  override fun doFilterInternal(request: HttpServletRequest, @NonNull response: HttpServletResponse, @NonNull filterChain: FilterChain) {

    if (excludeUriRegex.matcher(request.requestURI).matches()) {
      MDC.put(SKIP_LOGGING, "true")
    }

    try {
      val start = LocalDateTime.now()
      if (log.isTraceEnabled() && isLoggingAllowed) {
        log.trace("Request: {} {}", request.method, request.requestURI)
      }

      filterChain.doFilter(request, response)

      val duration = Duration.between(start, LocalDateTime.now()).toMillis()
      MDC.put(REQUEST_DURATION, duration.toString())
      val status = response.status
      MDC.put(RESPONSE_STATUS, status.toString())
      if (log.isTraceEnabled() && isLoggingAllowed) {
        log.trace("Response: {} {} - Status {} - Start {}, Duration {} ms", request.method, request.requestURI, status, start.format(formatter), duration)
      }
    } finally {
      MDC.remove(REQUEST_DURATION)
      MDC.remove(RESPONSE_STATUS)
      MDC.remove(SKIP_LOGGING)
    }
  }
}
