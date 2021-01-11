package uk.gov.justice.digital.hmpps.pollpush.services

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.DeleteMessageRequest
import com.amazonaws.services.sqs.model.GetQueueAttributesResult
import com.amazonaws.services.sqs.model.GetQueueUrlResult
import com.amazonaws.services.sqs.model.Message
import com.amazonaws.services.sqs.model.PurgeQueueRequest
import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import com.amazonaws.services.sqs.model.ReceiveMessageResult
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class QueueAdminServiceTest {

  private val awsSqsClient = mock<AmazonSQS>()
  private val awsSqsDlqClient = mock<AmazonSQS>()
  private val queueName = "queue"
  private val dlqName = "dlq"
  private val queueUrl = "arn:eu-west-1:queue"
  private val dlqUrl = "arn:eu-west-1:dlq"
  private lateinit var queueAdminService: QueueAdminService

  @BeforeEach
  internal fun setUp() {
    whenever(awsSqsClient.getQueueUrl(queueName)).thenReturn(GetQueueUrlResult().withQueueUrl(queueUrl))
    whenever(awsSqsDlqClient.getQueueUrl(dlqName)).thenReturn(GetQueueUrlResult().withQueueUrl(dlqUrl))
    queueAdminService = QueueAdminService(
      awsSqsClient = awsSqsClient,
      awsSqsDlqClient = awsSqsDlqClient,
      queueName = queueName,
      dlqName = dlqName,
    )
  }

  @Nested
  inner class PurgeDlq {
    @Test
    fun `will purge DLQ of all messages`() {
      whenever(awsSqsDlqClient.getQueueAttributes(dlqUrl, listOf("ApproximateNumberOfMessages")))
        .thenReturn(GetQueueAttributesResult().addAttributesEntry("ApproximateNumberOfMessages", "1"))

      queueAdminService.clearAllDlqMessages()

      verify(awsSqsDlqClient).purgeQueue(PurgeQueueRequest(dlqUrl))
    }

    @Test
    fun `will not purge DLQ if empty`() {
      whenever(awsSqsDlqClient.getQueueAttributes(dlqUrl, listOf("ApproximateNumberOfMessages")))
        .thenReturn(GetQueueAttributesResult().addAttributesEntry("ApproximateNumberOfMessages", "0"))

      queueAdminService.clearAllDlqMessages()

      verify(awsSqsDlqClient, never()).purgeQueue(PurgeQueueRequest(dlqUrl))
    }
  }

  @Nested
  inner class TransferDlq {
    @Test
    fun `will transfer a single message`() {
      whenever(awsSqsDlqClient.getQueueAttributes(dlqUrl, listOf("ApproximateNumberOfMessages")))
        .thenReturn(GetQueueAttributesResult().addAttributesEntry("ApproximateNumberOfMessages", "1"))
      whenever(awsSqsDlqClient.receiveMessage(any<ReceiveMessageRequest>()))
        .thenReturn(ReceiveMessageResult().withMessages(Message().withBody("some body").withReceiptHandle("some-receipt-handle")))

      queueAdminService.transferDlqMessages()

      verify(awsSqsDlqClient).receiveMessage(ReceiveMessageRequest().withQueueUrl(dlqUrl).withMaxNumberOfMessages(1))
      verify(awsSqsClient).sendMessage(queueUrl, "some body")
      verify(awsSqsDlqClient).deleteMessage(DeleteMessageRequest(dlqUrl, "some-receipt-handle"))
    }

    @Test
    fun `will transfer multiple messages`() {
      whenever(awsSqsDlqClient.getQueueAttributes(dlqUrl, listOf("ApproximateNumberOfMessages")))
        .thenReturn(GetQueueAttributesResult().addAttributesEntry("ApproximateNumberOfMessages", "3"))
      whenever(awsSqsDlqClient.receiveMessage(any<ReceiveMessageRequest>()))
        .thenReturn(ReceiveMessageResult().withMessages(Message().withBody("some body").withReceiptHandle("some-receipt-handle")))
        .thenReturn(ReceiveMessageResult().withMessages(Message().withBody("some body 2").withReceiptHandle("some-receipt-handle-2")))
        .thenReturn(ReceiveMessageResult().withMessages(Message().withBody("some body 3").withReceiptHandle("some-receipt-handle-3")))

      queueAdminService.transferDlqMessages()

      verify(awsSqsDlqClient, times(3)).receiveMessage(ReceiveMessageRequest().withQueueUrl(dlqUrl).withMaxNumberOfMessages(1))
      verify(awsSqsClient).sendMessage(queueUrl, "some body")
      verify(awsSqsClient).sendMessage(queueUrl, "some body 2")
      verify(awsSqsClient).sendMessage(queueUrl, "some body 3")
      verify(awsSqsDlqClient).deleteMessage(DeleteMessageRequest(dlqUrl, "some-receipt-handle"))
      verify(awsSqsDlqClient).deleteMessage(DeleteMessageRequest(dlqUrl, "some-receipt-handle-2"))
      verify(awsSqsDlqClient).deleteMessage(DeleteMessageRequest(dlqUrl, "some-receipt-handle-3"))
    }

    @Test
    fun `will do nothing if no messages`() {
      whenever(awsSqsDlqClient.getQueueAttributes(dlqUrl, listOf("ApproximateNumberOfMessages")))
        .thenReturn(GetQueueAttributesResult().addAttributesEntry("ApproximateNumberOfMessages", "0"))
      whenever(awsSqsDlqClient.receiveMessage(any<ReceiveMessageRequest>()))
        .thenReturn(ReceiveMessageResult().withMessages(Message().withBody("some body").withReceiptHandle("some-receipt-handle")))

      queueAdminService.transferDlqMessages()

      verify(awsSqsDlqClient, never()).receiveMessage(any<ReceiveMessageRequest>())
      verify(awsSqsClient, never()).sendMessage(any(), any())
      verify(awsSqsDlqClient, never()).deleteMessage(any())
    }
  }
}
