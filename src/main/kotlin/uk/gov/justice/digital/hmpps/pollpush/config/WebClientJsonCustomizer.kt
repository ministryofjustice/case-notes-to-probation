package uk.gov.justice.digital.hmpps.pollpush.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.http.MediaType
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient

@Component
class WebClientJsonCustomizer(private val om: ObjectMapper) : WebClientCustomizer {
  override fun customize(wcb: WebClient.Builder?) {
    val exchangeStrategies = ExchangeStrategies.builder().codecs { d ->
      d.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(om, MediaType.APPLICATION_JSON))
      d.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(om, MediaType.APPLICATION_JSON))
    }.build()
    wcb?.exchangeStrategies(exchangeStrategies)
  }
}
