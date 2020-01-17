package uk.gov.justice.digital.hmpps.pollpush.services.health

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest
import com.amazonaws.services.sqs.model.GetQueueAttributesResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component

@Component
class QueueHealth(@Autowired private val awsSqsClient: AmazonSQS,
                  @Autowired private val awsSqsDlqClient: AmazonSQS,
                  @Value("\${sqs.queue.name}") private val queueName: String?,
                  @Value("\${sqs.dlq.name}") private val dlqName: String?) : HealthIndicator {

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun health(): Health {
        val queueAttributes = try {
            val url = awsSqsClient.getQueueUrl(queueName)
            awsSqsClient.getQueueAttributes(GetQueueAttributesRequest(url.queueUrl))
        } catch (e: Exception) {
            log.error("Unable to retrieve queue attributes for queue '{}' due to exception:", queueName, e)
            return Health.Builder().down().build()
        }
        val details = mutableMapOf(
                "MessagesOnQueue" to queueAttributes.attributes["ApproximateNumberOfMessages"].toString(),
                "MessageInFlight" to queueAttributes.attributes["ApproximateNumberOfMessagesNotVisible"].toString()
        )

        details.putAll(getDlqDetails(queueAttributes))
        log.info("Found details for queue '{}': {}", queueName, details)

        return Health.Builder().up().withDetails(details.toMap()).build()
    }

    private fun getDlqDetails(mainQueueAttributes: GetQueueAttributesResult): Map<String, String> {
        if (!mainQueueAttributes.attributes.containsKey("RedrivePolicy")) {
            log.info("Queue '{}' is missing a RedrivePolicy attribute indicating it does not have a dead letter queue", queueName)
            return mapOf("dlq_status" to "DOWN")
        }

        val dlqAttributes = try {
            val url = awsSqsDlqClient.getQueueUrl(dlqName)
            awsSqsDlqClient.getQueueAttributes(GetQueueAttributesRequest(url.queueUrl))
        } catch (e: Exception) {
            log.info("Unable to retrieve dead letter queue attributes for queue '{}' due to exception:", queueName, e)
            return mapOf("dlq_status" to "DOWN")
        }

        return mapOf(
                "dlq_status" to "UP",
                "MessagesOnDlq" to dlqAttributes.attributes["ApproximateNumberOfMessages"].toString()
        )
    }

}