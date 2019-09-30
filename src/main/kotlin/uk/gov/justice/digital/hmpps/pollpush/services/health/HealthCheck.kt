package uk.gov.justice.digital.hmpps.pollpush.services.health

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

abstract class HealthCheck(val restTemplate: RestTemplate) : HealthIndicator {

  override fun health(): Health {
    try {
      val responseEntity = restTemplate.getForEntity("/ping", String::class.java)
      return Health.up().withDetail("HttpStatus", responseEntity.statusCode).build()
    } catch (e: RestClientException) {
      return Health.down(e).build()
    }
  }
}

@Component
class CaseNotesApiHealth
constructor(@Qualifier("caseNotesApiHealthRestTemplate") restTemplate: RestTemplate) : HealthCheck(restTemplate)

@Component
class OAuthApiHealth @Autowired
constructor(@Qualifier("oauthApiRestTemplate") restTemplate: RestTemplate) : HealthCheck(restTemplate)

