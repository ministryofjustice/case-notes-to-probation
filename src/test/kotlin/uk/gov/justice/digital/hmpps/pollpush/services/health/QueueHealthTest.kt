package uk.gov.justice.digital.hmpps.pollpush.services.health

import com.amazonaws.services.sqs.AmazonSQS
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
    private val amazonSqs: AmazonSQS = mock()
    private val queueHealth: QueueHealth = QueueHealth(amazonSqs, someQueueName)

    @Test
    fun `health - queue found - UP`() {
        val someQueueUrl = "some queue url"
        val urlResult = GetQueueUrlResult().withQueueUrl(someQueueUrl)
        whenever(amazonSqs.getQueueUrl(someQueueName)).thenReturn(urlResult)

        val health = queueHealth.health()

        assertThat(health.status).isEqualTo(Status.UP)
    }

    @Test
    fun `health - queue not found - DOWN`() {
        whenever(amazonSqs.getQueueUrl(anyString())).thenThrow(QueueDoesNotExistException::class.java)

        val health = queueHealth.health()

        assertThat(health.status).isEqualTo(Status.DOWN)
    }
}

