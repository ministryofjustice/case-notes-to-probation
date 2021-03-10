package uk.gov.justice.digital.hmpps.pollpush

import com.amazonaws.services.sqs.AmazonSQS
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.SpyBean
import uk.gov.justice.digital.hmpps.pollpush.integration.IntegrationTest

abstract class QueueIntegrationTest : IntegrationTest() {

  @SpyBean
  @Qualifier("awsSqsClient")
  internal lateinit var awsSqsClient: AmazonSQS

  @SpyBean
  @Qualifier("awsSqsDlqClient")
  internal lateinit var awsSqsDlqClient: AmazonSQS

  @Autowired
  @Value("\${sqs.queue.name}")
  internal lateinit var queueName: String

  @Autowired
  @Value("\${sqs.dlq.name}")
  internal lateinit var dlqName: String

  @BeforeEach
  internal fun setUp() {
    // wait until our queues have been purged
    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }
    await untilCallTo { getNumberOfMessagesCurrentlyOnDlq() } matches { it == 0 }
  }

  internal fun getQueueUrl(): String? {
    return awsSqsClient.getQueueUrl(queueName).queueUrl
  }

  internal fun getDlqUrl(): String? {
    return awsSqsDlqClient.getQueueUrl(dlqName).queueUrl
  }

  internal fun getNumberOfMessagesCurrentlyOnQueue(): Int? {
    val queueAttributes = awsSqsClient.getQueueAttributes(getQueueUrl(), listOf("ApproximateNumberOfMessages"))
    return queueAttributes.attributes["ApproximateNumberOfMessages"]?.toInt()
  }

  internal fun getNumberOfMessagesCurrentlyOnDlq(): Int? {
    val queueAttributes = awsSqsDlqClient.getQueueAttributes(getDlqUrl(), listOf("ApproximateNumberOfMessages"))
    return queueAttributes.attributes["ApproximateNumberOfMessages"]?.toInt()
  }
}
