package uk.gov.justice.digital.hmpps.pollpush.services.health

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest
import com.amazonaws.services.sqs.model.GetQueueAttributesResult
import com.amazonaws.services.sqs.model.GetQueueUrlResult
import com.amazonaws.services.sqs.model.QueueDoesNotExistException
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.boot.actuate.health.Status

class QueueHealthTest {

    private val someQueueName = "some queue name"
    private val someQueueUrl = "some queue url"
    private val someDlqName = "some dlq name"
    private val someDlqUrl = "some dlq url"
    private val someMessagesOnQueueCount = "123"
    private val someMessagesInFlightCount = "456"
    private val someMessagesOnDlqCount = "789"
    private val amazonSqs: AmazonSQS = mock()
    private val amazonSqsDlq: AmazonSQS = mock()
    private val queueHealth: QueueHealth = QueueHealth(amazonSqs, amazonSqsDlq, someQueueName, someDlqName)

    @Test
    fun `health - queue found - UP`() {
        whenever(amazonSqs.getQueueUrl(someQueueName)).thenReturn(someGetQueueUrlResult())
        whenever(amazonSqs.getQueueAttributes(someGetQueueAttributesRequest())).thenReturn(someGetQueueAttributesResult())

        val health = queueHealth.health()

        assertThat(health.status).isEqualTo(Status.UP)
    }

    @Test
    fun `health - attributes returned - included in health status`() {
        whenever(amazonSqs.getQueueUrl(someQueueName)).thenReturn(someGetQueueUrlResult())
        whenever(amazonSqs.getQueueAttributes(someGetQueueAttributesRequest())).thenReturn(someGetQueueAttributesResult())

        val health = queueHealth.health()

        assertThat(health.details["MessagesOnQueue"]).isEqualTo(someMessagesOnQueueCount)
        assertThat(health.details["MessageInFlight"]).isEqualTo(someMessagesInFlightCount)
    }

    @Test
    fun `health - queue not found - DOWN`() {
        whenever(amazonSqs.getQueueUrl(anyString())).thenThrow(QueueDoesNotExistException::class.java)

        val health = queueHealth.health()

        assertThat(health.status).isEqualTo(Status.DOWN)
    }

    @Test
    fun `health - failed to get attributes - DOWN`() {
        whenever(amazonSqs.getQueueUrl(anyString())).thenReturn(someGetQueueUrlResult())
        whenever(amazonSqs.getQueueAttributes(someGetQueueAttributesRequest())).thenThrow(RuntimeException::class.java)

        val health = queueHealth.health()

        assertThat(health.status).isEqualTo(Status.DOWN)
    }

    @Test
    fun `health - dlq attributes returned - included in health status`() {
        whenever(amazonSqs.getQueueUrl(someQueueName)).thenReturn(someGetQueueUrlResult())
        whenever(amazonSqs.getQueueAttributes(someGetQueueAttributesRequest())).thenReturn(someGetQueueAttributesResultWithDlq())
        whenever(amazonSqsDlq.getQueueUrl(someDlqName)).thenReturn(someGetQueueUrlResultForDlq())
        whenever(amazonSqsDlq.getQueueAttributes(someGetQueueAttributesRequestForDlq())).thenReturn(someGetQueueAttributesResultForDlq())

        val health = queueHealth.health()

        assertThat(health.status).isEqualTo(Status.UP)
        assertThat(health.details["dlq_status"]).isEqualTo("UP")
        assertThat(health.details["MessagesOnDlq"]).isEqualTo(someMessagesOnDlqCount)
    }

    @Test
    fun `health - no RedrivePolicy attribute - DLQ DOWN`() {
        whenever(amazonSqs.getQueueUrl(someQueueName)).thenReturn(someGetQueueUrlResult())
        whenever(amazonSqs.getQueueAttributes(someGetQueueAttributesRequest())).thenReturn(someGetQueueAttributesResult())

        val health = queueHealth.health()

        assertThat(health.status).isEqualTo(Status.UP)
        assertThat(health.details["dlq_status"]).isEqualTo("DOWN")
    }

    @Test
    fun `health - dlq not found - DLQ DOWN`() {
        whenever(amazonSqs.getQueueUrl(someQueueName)).thenReturn(someGetQueueUrlResult())
        whenever(amazonSqs.getQueueAttributes(someGetQueueAttributesRequest())).thenReturn(someGetQueueAttributesResultWithDlq())
        whenever(amazonSqsDlq.getQueueUrl(someDlqName)).thenThrow(QueueDoesNotExistException::class.java)

        val health = queueHealth.health()

        assertThat(health.status).isEqualTo(Status.UP)
        assertThat(health.details["dlq_status"]).isEqualTo("DOWN")
    }

    @Test
    fun `health - dlq failed to get attributes - DLQ DOWN`() {
        whenever(amazonSqs.getQueueUrl(someQueueName)).thenReturn(someGetQueueUrlResult())
        whenever(amazonSqs.getQueueAttributes(someGetQueueAttributesRequest())).thenReturn(someGetQueueAttributesResultWithDlq())
        whenever(amazonSqsDlq.getQueueUrl(someDlqName)).thenReturn(someGetQueueUrlResultForDlq())
        whenever(amazonSqsDlq.getQueueAttributes(someGetQueueAttributesRequestForDlq())).thenThrow(RuntimeException::class.java)

        val health = queueHealth.health()

        assertThat(health.status).isEqualTo(Status.UP)
        assertThat(health.details["dlq_status"]).isEqualTo("DOWN")
    }

    private fun someGetQueueAttributesRequest() = GetQueueAttributesRequest(someQueueUrl)
    private fun someGetQueueUrlResult(): GetQueueUrlResult = GetQueueUrlResult().withQueueUrl(someQueueUrl)
    private fun someGetQueueAttributesResult() = GetQueueAttributesResult().withAttributes(
            mapOf("ApproximateNumberOfMessages" to someMessagesOnQueueCount,
                    "ApproximateNumberOfMessagesNotVisible" to someMessagesInFlightCount))
    private fun someGetQueueAttributesResultWithDlq() = GetQueueAttributesResult().withAttributes(
            mapOf("ApproximateNumberOfMessages" to someMessagesOnQueueCount,
                    "ApproximateNumberOfMessagesNotVisible" to someMessagesInFlightCount,
                    "RedrivePolicy" to "any redrive policy"))

    private fun someGetQueueAttributesRequestForDlq() = GetQueueAttributesRequest(someDlqUrl)
    private fun someGetQueueUrlResultForDlq(): GetQueueUrlResult = GetQueueUrlResult().withQueueUrl(someDlqUrl)
    private fun someGetQueueAttributesResultForDlq() = GetQueueAttributesResult().withAttributes(
            mapOf("ApproximateNumberOfMessages" to someMessagesOnDlqCount))

}

