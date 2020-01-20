package uk.gov.justice.digital.hmpps.pollpush.config

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.CreateQueueRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("test-queue")
open class TestQueueConfig(
                           private val sqsClient: AmazonSQS,
                           @Value("\${sqs.queue.name}") private val queueName: String,
                           @Value("\${sqs.dlq.name}") private val dlqName: String) {
    @Bean
    open fun queueUrl(): String {
        sqsClient.createQueue(CreateQueueRequest(queueName))
        sqsClient.createQueue(CreateQueueRequest(dlqName))
        return sqsClient.getQueueUrl(queueName).queueUrl
    }

}
