package uk.gov.justice.digital.hmpps.pollpush.services.health

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.info.BuildProperties
import org.springframework.stereotype.Component

/**
 * Adds version data to the /health endpoint. This is called by the UI to display API details
 */
@Component
class HealthInfo(@param:Autowired(required = false) private val buildProperties: BuildProperties?) : HealthIndicator {
  private val version: String = if (buildProperties == null) "version not available" else buildProperties.version

  override fun health(): Health = Health.up().withDetail("version", version).build()
}
