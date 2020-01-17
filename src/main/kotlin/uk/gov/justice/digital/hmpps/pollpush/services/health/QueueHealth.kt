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
        val url = try {
            amazonSQS.getQueueUrl(queueName)
        } catch(e: Exception) {
            log.error("Unable to retrieve queue url for queue {} due to exception", queueName, e)
            return Health.Builder().down().build()
        }
        val response = amazonSQS.getQueueAttributes(GetQueueAttributesRequest(url.queueUrl))
        return Health.Builder().up().build()
    }

}