package uk.gov.justice.digital.hmpps.pollpush.config

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.CreateQueueRequest
import com.amazonaws.services.sqs.model.QueueAttributeName
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnExpression("'\${sqs.provider}'.equals('localstack')")
open class TestQueueConfig(
    private val awsSqsClient: AmazonSQS,
    @Value("\${sqs.queue.name}") private val queueName: String,
    @Value("\${sqs.dlq.name}") private val dlqName: String,
    @Value("\${sqs.provider}") private val provider: String,
    @Value("\${sqs.embedded}") private val embedded: String) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Bean
  open fun queueUrl(): String {
    log.info("sqs.provider=${provider}, embedded=${embedded}")
    val result = awsSqsClient.createQueue(CreateQueueRequest(dlqName))
    val dlqArn = awsSqsClient.getQueueAttributes(result.queueUrl, listOf(QueueAttributeName.QueueArn.toString()))
    awsSqsClient.createQueue(CreateQueueRequest(queueName).withAttributes(
        mapOf(QueueAttributeName.RedrivePolicy.toString() to
            """{"deadLetterTargetArn":"${dlqArn.attributes["QueueArn"]}","maxReceiveCount":"5"}""")
    ))
    return awsSqsClient.getQueueUrl(queueName).queueUrl
  }

}
