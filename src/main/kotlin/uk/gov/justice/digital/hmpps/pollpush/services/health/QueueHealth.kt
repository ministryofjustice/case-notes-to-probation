package uk.gov.justice.digital.hmpps.pollpush.services.health

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component

enum class DlqStatus(val description: String) {
    UP("UP"),
    NOT_ATTACHED("The queue does not have a dead letter queue attached"),
    NOT_FOUND("The queue does not exist"),
    NOT_AVAILABLE("The queue cannot be interrogated")
}

enum class QueueAttributes(val awsName: String, val healthName: String) {
    MESSAGES_ON_QUEUE(QueueAttributeName.ApproximateNumberOfMessages.toString(), "MessagesOnQueue"),
    MESSAGES_IN_FLIGHT(QueueAttributeName.ApproximateNumberOfMessagesNotVisible.toString(), "MessagesInFlight"),
    MESSAGES_ON_DLQ(QueueAttributeName.ApproximateNumberOfMessages.toString(), "MessagesOnDLQ")
}

@Component
class QueueHealth(@Autowired @Qualifier("awsSqsClient") private val awsSqsClient: AmazonSQS,
                  @Autowired @Qualifier("awsSqsDlqClient") private val awsSqsDlqClient: AmazonSQS,
                  @Value("\${sqs.queue.name}") private val queueName: String?,
                  @Value("\${sqs.dlq.name}") private val dlqName: String?) : HealthIndicator {

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun health(): Health {
        val queueAttributes = try {
            val url = awsSqsClient.getQueueUrl(queueName)
            awsSqsClient.getQueueAttributes(getQueueAttributesRequest(url))
        } catch (e: Exception) {
            log.error("Unable to retrieve queue attributes for queue '{}' due to exception:", queueName, e)
            return Health.Builder().down().build()
        }
        val details = mutableMapOf<String, Any?>(
                QueueAttributes.MESSAGES_ON_QUEUE.healthName to queueAttributes.attributes[QueueAttributes.MESSAGES_ON_QUEUE.awsName]?.toInt(),
                QueueAttributes.MESSAGES_IN_FLIGHT.healthName to queueAttributes.attributes[QueueAttributes.MESSAGES_IN_FLIGHT.awsName]?.toInt()
        )

        details.putAll(getDlqHealth(queueAttributes))
        log.info("Found details for queue '{}': {}", queueName, details)

        return Health.Builder().up().withDetails(details.toMap()).build()
    }

    private fun getDlqHealth(mainQueueAttributes: GetQueueAttributesResult): Map<String, Any?> {
        if (!mainQueueAttributes.attributes.containsKey("RedrivePolicy")) {
            log.info("Queue '{}' is missing a RedrivePolicy attribute indicating it does not have a dead letter queue", queueName)
            return mapOf("dlqStatus" to DlqStatus.NOT_ATTACHED.description)
        }

        val dlqAttributes = try {
            val url = awsSqsDlqClient.getQueueUrl(dlqName)
            awsSqsDlqClient.getQueueAttributes(getQueueAttributesRequest(url))
        } catch (e: QueueDoesNotExistException) {
            log.info("Unable to retrieve dead letter queue URL for queue '{}' due to exception:", queueName, e)
            return mapOf("dlqStatus" to DlqStatus.NOT_FOUND.description)
        } catch (e: Exception) {
            log.info("Unable to retrieve dead letter queue attributes for queue '{}' due to exception:", queueName, e)
            return mapOf("dlqStatus" to DlqStatus.NOT_AVAILABLE.description)
        }

        return mapOf(
                "dlqStatus" to DlqStatus.UP.description,
                QueueAttributes.MESSAGES_ON_DLQ.healthName to dlqAttributes.attributes[QueueAttributes.MESSAGES_ON_DLQ.awsName]?.toInt()
        )
    }

    private fun getQueueAttributesRequest(url: GetQueueUrlResult) =
        GetQueueAttributesRequest(url.queueUrl).withAttributeNames(QueueAttributeName.All)

}
