package uk.gov.justice.digital.hmpps.pollpush.utils

import com.microsoft.applicationinsights.web.internal.correlation.TraceContextCorrelation
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.lang.NonNull
import java.io.IOException

/**
 * Temporary hack to pass w3c tracing headers.  Once https://github.com/microsoft/ApplicationInsights-Java/issues/674 has
 * been fixed can be removed and switch to apache httpclient instead.
 */
class W3cTracingInterceptor : ClientHttpRequestInterceptor {

  @NonNull
  @Throws(IOException::class)
  override fun intercept(
      request: HttpRequest, @NonNull body: ByteArray, @NonNull execution: ClientHttpRequestExecution): ClientHttpResponse {

    val headers = request.headers
    headers.add("traceparent", TraceContextCorrelation.generateChildDependencyTraceparent())
    val tracestate = TraceContextCorrelation.retriveTracestate()
    if (tracestate != null) {
      headers.add("tracestate", tracestate)
    }

    return execution.execute(request, body)
  }
}
