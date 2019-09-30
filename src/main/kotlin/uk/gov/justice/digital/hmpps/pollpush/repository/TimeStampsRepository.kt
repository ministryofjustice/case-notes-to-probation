package uk.gov.justice.digital.hmpps.pollpush.repository

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.repository.MongoRepository


interface TimeStampsRepository : MongoRepository<TimeStamps, String>

// value should really be a local date time, but need to read existing rows so left as string
data class TimeStamps(@Id val id: String, val value: String)
