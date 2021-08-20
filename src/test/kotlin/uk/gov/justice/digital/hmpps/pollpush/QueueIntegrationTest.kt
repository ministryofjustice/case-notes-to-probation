package uk.gov.justice.digital.hmpps.pollpush

import com.amazonaws.services.sqs.model.PurgeQueueRequest
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import uk.gov.justice.digital.hmpps.pollpush.LocalStackContainer.setLocalStackProperties
import uk.gov.justice.digital.hmpps.pollpush.integration.IntegrationTest
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService

abstract class QueueIntegrationTest : IntegrationTest() {
  @Autowired
  protected lateinit var hmppsQueueService: HmppsQueueService

  internal val eventQueue by lazy { hmppsQueueService.findByQueueId("events") as HmppsQueue }
  internal val awsSqsClient by lazy { eventQueue.sqsClient }
  internal val awsSqsDlqClient by lazy {
    eventQueue.sqsDlqClient ?: throw RuntimeException("DLQ doesn't exist")
  }
  internal val queueUrl by lazy { eventQueue.queueUrl }
  internal val queueName by lazy { eventQueue.queueName }
  internal val dlqName by lazy { eventQueue.dlqName }
  internal val dlqUrl by lazy { eventQueue.dlqUrl }

  @BeforeEach
  fun cleanQueues() {
    awsSqsClient.purgeQueue(PurgeQueueRequest(queueUrl))
    awsSqsDlqClient.purgeQueue(PurgeQueueRequest(dlqUrl))
  }

  internal fun getNumberOfMessagesCurrentlyOnQueue(): Int? {
    val queueAttributes = awsSqsClient.getQueueAttributes(queueUrl, listOf("ApproximateNumberOfMessages"))
    return queueAttributes.attributes["ApproximateNumberOfMessages"]?.toInt()
  }

  internal fun getNumberOfMessagesCurrentlyOnDlq(): Int? {
    val queueAttributes = awsSqsDlqClient.getQueueAttributes(dlqUrl, listOf("ApproximateNumberOfMessages"))
    return queueAttributes.attributes["ApproximateNumberOfMessages"]?.toInt()
  }

  companion object {
    private val localStackContainer = LocalStackContainer.instance

    @JvmStatic
    @DynamicPropertySource
    fun testcontainers(registry: DynamicPropertyRegistry) {
      localStackContainer?.also { setLocalStackProperties(it, registry) }
    }
  }
}
