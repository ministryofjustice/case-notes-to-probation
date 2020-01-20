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
    private val awsSqsClient: AmazonSQS,
    @Value("\${sqs.queue.name}") private val queueName: String,
    @Value("\${sqs.dlq.name}") private val dlqName: String) {
    @Bean
    open fun queueUrl(): String {
        awsSqsClient.createQueue(CreateQueueRequest(queueName))
        awsSqsClient.createQueue(CreateQueueRequest(dlqName))
        return awsSqsClient.getQueueUrl(queueName).queueUrl
    }

}
