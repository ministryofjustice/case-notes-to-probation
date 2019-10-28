package uk.gov.justice.digital.hmpps.pollpush

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer

@SpringBootApplication
@EnableResourceServer
open class PollPushApplication

fun main(args: Array<String>) {
  runApplication<PollPushApplication>(*args)
}

@Configuration
@EnableScheduling
open class SchedulingConfiguration
