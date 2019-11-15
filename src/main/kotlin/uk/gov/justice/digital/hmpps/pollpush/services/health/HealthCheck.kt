package uk.gov.justice.digital.hmpps.pollpush.services.health

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

abstract class HealthCheck(private val restTemplate: RestTemplate) : HealthIndicator {

  override fun health(): Health {
    return try {
      val responseEntity = restTemplate.getForEntity("/ping", String::class.java)
      Health.up().withDetail("HttpStatus", responseEntity.statusCode).build()
    } catch (e: RestClientException) {
      Health.down(e).build()
    }
  }
}

@Component
class CaseNotesApiHealth
constructor(@Qualifier("caseNotesApiHealthRestTemplate") restTemplate: RestTemplate) : HealthCheck(restTemplate)

@Component
class DeliusApiHealth
constructor(@Qualifier("deliusApiHealthRestTemplate") restTemplate: RestTemplate) : HealthCheck(restTemplate)

@Component
class OAuthApiHealth
constructor(@Qualifier("oauthApiRestTemplate") restTemplate: RestTemplate) : HealthCheck(restTemplate)

