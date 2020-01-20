package uk.gov.justice.digital.hmpps.pollpush.services.health

import com.amazonaws.services.sqs.model.GetQueueAttributesRequest
import com.amazonaws.services.sqs.model.GetQueueAttributesResult
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.nhaarman.mockito_kotlin.whenever
import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.util.ReflectionTestUtils


class HealthCheckIntegrationTest : IntegrationTest() {

  @Autowired
  private lateinit var queueHealth: QueueHealth

  @Test
  fun `Health page reports ok`() {
    subPing(200)

    val response = restTemplate.getForEntity("/health", String::class.java)

    assertThatJson(response.body).node("components.OAuthApiHealth.details.HttpStatus").isEqualTo("OK")
    assertThatJson(response.body).node("components.caseNotesApiHealth.details.HttpStatus").isEqualTo("OK")
    assertThatJson(response.body).node("components.deliusApiHealth.details.HttpStatus").isEqualTo("OK")
    assertThatJson(response.body).node("status").isEqualTo("UP")
    assertThat(response.statusCodeValue).isEqualTo(200)
  }

  @Test
  fun `Health ping page is accessible`() {
    subPing(200)

    val response = restTemplate.getForEntity("/health/ping", String::class.java)

    assertThatJson(response.body).node("status").isEqualTo("UP")
    assertThat(response.statusCodeValue).isEqualTo(200)
  }

  @Test
  fun `Health page reports down`() {
    subPing(404)

    val response = restTemplate.getForEntity("/health", String::class.java)

    assertThatJson(response.body).node("components.OAuthApiHealth.details.error").isEqualTo("org.springframework.web.client.HttpClientErrorException\$NotFound: 404 Not Found: [some error]")
    assertThatJson(response.body).node("components.caseNotesApiHealth.details.error").isEqualTo("org.springframework.web.client.HttpClientErrorException\$NotFound: 404 Not Found: [some error]")
    assertThatJson(response.body).node("components.deliusApiHealth.details.error").isEqualTo("org.springframework.web.client.HttpClientErrorException\$NotFound: 404 Not Found: [some error]")
    assertThatJson(response.body).node("status").isEqualTo("DOWN")
    assertThat(response.statusCodeValue).isEqualTo(503)
  }

  @Test
  fun `Health page reports a teapot`() {
    subPing(418)

    val response = restTemplate.getForEntity("/health", String::class.java)

    assertThatJson(response.body).node("components.OAuthApiHealth.details.error").isEqualTo("org.springframework.web.client.HttpClientErrorException: 418 418: [some error]")
    assertThatJson(response.body).node("components.caseNotesApiHealth.details.error").isEqualTo("org.springframework.web.client.HttpClientErrorException: 418 418: [some error]")
    assertThatJson(response.body).node("components.deliusApiHealth.details.error").isEqualTo("org.springframework.web.client.HttpClientErrorException: 418 418: [some error]")
    assertThatJson(response.body).node("status").isEqualTo("DOWN")
    assertThat(response.statusCodeValue).isEqualTo(503)
  }

  @Test
  fun `Queue Health page reports ok`() {
    subPing(200)

    val response = restTemplate.getForEntity("/health", String::class.java)

    assertThatJson(response.body).node("components.queueHealth.status").isEqualTo("UP")
    assertThatJson(response.body).node("status").isEqualTo("UP")
    assertThat(response.statusCodeValue).isEqualTo(200)
  }

  @Test
  fun `Queue does not exist reports down`() {
    val realQueueName = ReflectionTestUtils.getField(queueHealth, "queueName") as String
    ReflectionTestUtils.setField(queueHealth, "queueName", "missing_queue")
    subPing(200)

    val response = restTemplate.getForEntity("/health", String::class.java)

    assertThatJson(response.body).node("status").isEqualTo("DOWN")
    assertThatJson(response.body).node("components.queueHealth.status").isEqualTo("DOWN")
    assertThat(response.statusCodeValue).isEqualTo(503)

    ReflectionTestUtils.setField(queueHealth, "queueName", realQueueName)
  }

  @Test
  fun `Queue health ok and dlq health ok, reports everything up`() {
    subPing(200)
    mockQueueWithRedrivePolicyAttributes()

    val response = restTemplate.getForEntity("/health", String::class.java)

    assertThatJson(response.body).node("status").isEqualTo("UP")
    assertThatJson(response.body).node("components.queueHealth.status").isEqualTo("UP")
    assertThatJson(response.body).node("components.queueHealth.details.dlqStatus").isEqualTo("UP")
    assertThat(response.statusCodeValue).isEqualTo(200)
  }

  @Test
  fun `Dlq down does not bring main health or queue health down`() {
    subPing(200)

    val response = restTemplate.getForEntity("/health", String::class.java)

    assertThatJson(response.body).node("status").isEqualTo("UP")
    assertThatJson(response.body).node("components.queueHealth.status").isEqualTo("UP")
    assertThatJson(response.body).node("components.queueHealth.details.dlqStatus").isEqualTo("DOWN")
    assertThat(response.statusCodeValue).isEqualTo(200)
  }

  @Test
  fun `Main queue has no redrive policy reports dlq down`() {
    subPing(200)

    val response = restTemplate.getForEntity("/health", String::class.java)

    assertThatJson(response.body).node("components.queueHealth.details.dlqStatus").isEqualTo("DOWN")
    assertThat(response.statusCodeValue).isEqualTo(200)
  }

  @Test
  fun `Dlq not found reports dlq down`() {
    subPing(200)
    mockQueueWithRedrivePolicyAttributes()
    val realDlqName = ReflectionTestUtils.getField(queueHealth, "queueName")
    ReflectionTestUtils.setField(queueHealth, "dlqName", "missing_queue")

    val response = restTemplate.getForEntity("/health", String::class.java)

    assertThatJson(response.body).node("components.queueHealth.details.dlqStatus").isEqualTo("DOWN")
    assertThat(response.statusCodeValue).isEqualTo(200)

    ReflectionTestUtils.setField(queueHealth, "dlqName", realDlqName)
  }

  private fun subPing(status: Int) {
    oauthMockServer.stubFor(get("/auth/ping").willReturn(aResponse()
        .withHeader("Content-Type", "application/json")
        .withBody(if (status == 200) "pong" else "some error")
        .withStatus(status)))

    caseNotesMockServer.stubFor(get("/ping").willReturn(aResponse()
        .withHeader("Content-Type", "application/json")
        .withBody(if (status == 200) "pong" else "some error")
        .withStatus(status)))

    deliusMockServer.stubFor(get("/ping").willReturn(aResponse()
        .withHeader("Content-Type", "application/json")
        .withBody(if (status == 200) "pong" else "some error")
        .withStatus(status)))
  }

  private fun getQueueAttributesWithRedrivePolicy(): GetQueueAttributesResult =
    GetQueueAttributesResult().withAttributes(
            mapOf("RedrivePolicy" to "{\"deadLetterTargetArn\":\"arn:aws:sqs:eu-west-2:000000000000:case_notes_dlq\",\"maxReceiveCount\":\"1000\"}"))

  private fun mockQueueWithRedrivePolicyAttributes() {
    val queueName = ReflectionTestUtils.getField(queueHealth, "queueName") as String
    val queueUrl = awsSqsClient.getQueueUrl(queueName)
    whenever(awsSqsClient.getQueueAttributes(GetQueueAttributesRequest(queueUrl.queueUrl))).thenReturn(getQueueAttributesWithRedrivePolicy())
  }

}
