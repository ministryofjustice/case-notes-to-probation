package uk.gov.justice.digital.hmpps.pollpush.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.boot.web.client.RootUriTemplateHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2RestTemplate
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails
import org.springframework.web.client.RestTemplate

@Configuration
open class RestTemplateConfiguration(private val caseNotesApiDetails: ClientCredentialsResourceDetails,
                                     @Value("\${delius.endpoint.url}") private val deliusRootUri: String,
                                     @Value("\${casenotes.endpoint.url}") private val caseNotesRootUri: String,
                                     @Value("\${oauth.endpoint.url}") private val oauthRootUri: String,
                                     @Value("\${delius.username}") private val deliusUsername: String,
                                     @Value("\${delius.password}") private val deliusPassword: String) {
  @Bean(name = ["deliusApiRestTemplate"])
  open fun deliusRestTemplate(restTemplateBuilder: RestTemplateBuilder): RestTemplate =
      restTemplateBuilder
          .rootUri(deliusRootUri)
          .basicAuthentication(deliusUsername, deliusPassword)
          .build()

  @Bean(name = ["oauthApiRestTemplate"])
  open fun oauthRestTemplate(restTemplateBuilder: RestTemplateBuilder): RestTemplate =
      getRestTemplate(restTemplateBuilder, oauthRootUri)

  @Bean(name = ["caseNotesApiHealthRestTemplate"])
  open fun caseNotesHealthRestTemplate(restTemplateBuilder: RestTemplateBuilder): RestTemplate =
      getRestTemplate(restTemplateBuilder, caseNotesRootUri)

  private fun getRestTemplate(restTemplateBuilder: RestTemplateBuilder, uri: String?): RestTemplate =
      restTemplateBuilder.rootUri(uri).build()

  @Bean(name = ["caseNotesApiRestTemplate"])
  open fun caseNotesApiRestTemplate(): OAuth2RestTemplate {

    val caseNotesApiRestTemplate = OAuth2RestTemplate(caseNotesApiDetails)
    RootUriTemplateHandler.addTo(caseNotesApiRestTemplate, caseNotesRootUri)

    return caseNotesApiRestTemplate
  }
}
