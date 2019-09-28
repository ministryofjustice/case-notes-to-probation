package uk.gov.justice.digital.hmpps.pollpush

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer

@SpringBootApplication(exclude = [EmbeddedMongoAutoConfiguration::class])
@EnableResourceServer
open class PollPushApplication

fun main(args: Array<String>) {
  runApplication<PollPushApplication>(*args)
}


@Configuration
@Profile("dev")
@Import(EmbeddedMongoAutoConfiguration::class)
open class EmbededMongoConfiguration


@Configuration
@EnableScheduling
open class SchedulingConfiguration
