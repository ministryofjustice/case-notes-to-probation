package uk.gov.justice.digital.hmpps.pollpush.config

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.AnonymousAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import com.amazonaws.services.sqs.model.CreateQueueRequest
import com.amazonaws.services.sqs.model.QueueAttributeName
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jms.annotation.EnableJms

@Configuration
@EnableJms
@ConditionalOnProperty(name = ["sqs.provider"], havingValue = "localstack")
class JmsLocalStackConfig {

  @Bean("awsSqsClient")
  fun awsSqsClientLocalstack(
    @Value("\${sqs.endpoint.url}") serviceEndpoint: String,
    @Value("\${sqs.endpoint.region}") region: String
  ): AmazonSQS =
    AmazonSQSClientBuilder.standard()
      .withEndpointConfiguration(EndpointConfiguration(serviceEndpoint, region))
      .withCredentials(AWSStaticCredentialsProvider(AnonymousAWSCredentials()))
      .build()

  @Bean("awsSqsDlqClient")
  fun awsSqsDlqClientLocalstack(
    @Value("\${sqs.endpoint.url}") serviceEndpoint: String,
    @Value("\${sqs.endpoint.region}") region: String
  ): AmazonSQS =
    AmazonSQSClientBuilder.standard()
      .withEndpointConfiguration(EndpointConfiguration(serviceEndpoint, region))
      .withCredentials(AWSStaticCredentialsProvider(AnonymousAWSCredentials()))
      .build()

  @Bean
  fun queueUrl(
    @Qualifier("awsSqsClient") awsSqsClient: AmazonSQS,
    @Value("\${sqs.queue.name}") queueName: String,
    @Value("\${sqs.dlq.name}") dlqName: String
  ): String {
    val result = awsSqsClient.createQueue(CreateQueueRequest(dlqName))
    val dlqArn = awsSqsClient.getQueueAttributes(result.queueUrl, listOf(QueueAttributeName.QueueArn.toString()))
    awsSqsClient.createQueue(
      CreateQueueRequest(queueName).withAttributes(
        mapOf(
          QueueAttributeName.RedrivePolicy.toString() to
            """{"deadLetterTargetArn":"${dlqArn.attributes["QueueArn"]}","maxReceiveCount":"5"}"""
        )
      )
    )
    return awsSqsClient.getQueueUrl(queueName).queueUrl
  }
}
