package uk.gov.justice.digital.hmpps.pollpush.config

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import org.springframework.boot.jackson.JsonComponent
import java.io.IOException
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.OffsetTime

@JsonComponent
class OffsetDateTimeDeserializer : JsonDeserializer<OffsetDateTime?>() {
  @Throws(IOException::class, JsonProcessingException::class)
  override fun deserialize(parser: JsonParser, context: DeserializationContext?): OffsetDateTime {
    val json = parser.text
    return if (json.matches(".*(\\+|-)\\d{2}(:?\\d{2})?$".toRegex())) {
      OffsetDateTime.parse(json)
    } else {
      val offset = OffsetTime.now().offset
      OffsetDateTime.of(LocalDateTime.parse(json), offset)
    }
  }
}
