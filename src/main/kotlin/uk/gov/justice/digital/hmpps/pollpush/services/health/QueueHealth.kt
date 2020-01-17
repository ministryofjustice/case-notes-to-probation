package uk.gov.justice.digital.hmpps.pollpush.services.health

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component

@Component
class QueueHealth(@Autowired private val amazonSQS: AmazonSQS,
                  @Value("\${sqs.queue.name}") private val queueName: String?) : HealthIndicator {

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun health(): Health {
        val queueAttributes = try {
            val url = amazonSQS.getQueueUrl(queueName)
            amazonSQS.getQueueAttributes(GetQueueAttributesRequest(url.queueUrl))
        } catch (e: Exception) {
            log.error("Unable to retrieve queue attributes for queue '{}' due to exception:", queueName, e)
            return Health.Builder().down().build()
        }
        val details = mapOf(
                "MessagesOnQueue" to queueAttributes.attributes["ApproximateNumberOfMessages"].toString(),
                "MessageInFlight" to queueAttributes.attributes["ApproximateNumberOfMessagesNotVisible"].toString()
                )
        log.info("Found attributes for queue '{}': {}", queueName, details)
        return Health.Builder().up().withDetails(details).build()
    }

}