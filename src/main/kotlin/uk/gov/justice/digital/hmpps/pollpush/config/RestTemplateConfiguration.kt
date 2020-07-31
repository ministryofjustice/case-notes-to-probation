package uk.gov.justice.digital.hmpps.pollpush.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.boot.web.client.RootUriTemplateHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.client.OAuth2RestTemplate
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails
import org.springframework.web.client.DefaultResponseErrorHandler
import org.springframework.web.client.RestTemplate

@Configuration
class RestTemplateConfiguration(private val apiDetails: ClientCredentialsResourceDetails,
                                @Value("\${delius.endpoint.url}") private val deliusRootUri: String,
                                @Value("\${casenotes.endpoint.url}") private val caseNotesRootUri: String,
                                @Value("\${oauth.endpoint.url}") private val oauthRootUri: String) {
  @Bean(name = ["oauthApiRestTemplate"])
  fun oauthRestTemplate(restTemplateBuilder: RestTemplateBuilder): RestTemplate =
      getRestTemplate(restTemplateBuilder, oauthRootUri)

  @Bean(name = ["deliusApiRestTemplate"])
  fun deliusRestTemplate(): OAuth2RestTemplate {

    val deliusApiRestTemplate = OAuth2RestTemplate(apiDetails)
    RootUriTemplateHandler.addTo(deliusApiRestTemplate, deliusRootUri)
    deliusApiRestTemplate.errorHandler = NotFoundAndConflictIgnoringResponseErrorHandler()

    return deliusApiRestTemplate
  }

  @Bean(name = ["deliusApiHealthRestTemplate"])
  fun deliusHealthRestTemplate(restTemplateBuilder: RestTemplateBuilder): RestTemplate =
      getRestTemplate(restTemplateBuilder, deliusRootUri)

  @Bean(name = ["caseNotesApiHealthRestTemplate"])
  fun caseNotesHealthRestTemplate(restTemplateBuilder: RestTemplateBuilder): RestTemplate =
      getRestTemplate(restTemplateBuilder, caseNotesRootUri)

  @Bean(name = ["caseNotesApiRestTemplate"])
  fun caseNotesApiRestTemplate(): OAuth2RestTemplate {

    val caseNotesApiRestTemplate = OAuth2RestTemplate(apiDetails)
    RootUriTemplateHandler.addTo(caseNotesApiRestTemplate, caseNotesRootUri)

    return caseNotesApiRestTemplate
  }

  private fun getRestTemplate(restTemplateBuilder: RestTemplateBuilder, uri: String?): RestTemplate =
      restTemplateBuilder.rootUri(uri).build()
}

class NotFoundAndConflictIgnoringResponseErrorHandler : DefaultResponseErrorHandler() {
  // ignore not found and conflict errors as they are very frequent and fill up the logs
  // don't need to retry on those errors either
  override fun hasError(statusCode: HttpStatus): Boolean =
      statusCode.isError && statusCode != HttpStatus.NOT_FOUND && statusCode != HttpStatus.CONFLICT
}
