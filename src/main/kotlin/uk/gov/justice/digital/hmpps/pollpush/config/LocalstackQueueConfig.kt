package uk.gov.justice.digital.hmpps.pollpush.config

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.CreateQueueRequest
import com.amazonaws.services.sqs.model.QueueAttributeName
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("localstack")
open class LocalstackQueueConfig(
    private val awsSqsClient: AmazonSQS,
    @Value("\${sqs.queue.name}") private val queueName: String,
    @Value("\${sqs.dlq.name}") private val dlqName: String) {

    @Bean
    open fun queueUrl(): String {
      val result = awsSqsClient.createQueue(CreateQueueRequest(dlqName))
      val dlqArn = awsSqsClient.getQueueAttributes(result.queueUrl, listOf(QueueAttributeName.QueueArn.toString()))
      awsSqsClient.createQueue(CreateQueueRequest(queueName).withAttributes(
          mapOf(QueueAttributeName.RedrivePolicy.toString() to
              """{"deadLetterTargetArn":"${dlqArn.attributes["QueueArn"]}","maxReceiveCount":"5"}""")
      ))
        return awsSqsClient.getQueueUrl(queueName).queueUrl
    }

}
