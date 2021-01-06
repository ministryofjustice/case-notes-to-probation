package uk.gov.justice.digital.hmpps.pollpush

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
class PollPushApplication

fun main(args: Array<String>) {
  runApplication<PollPushApplication>(*args)
}

@Configuration
@EnableScheduling
class SchedulingConfiguration
