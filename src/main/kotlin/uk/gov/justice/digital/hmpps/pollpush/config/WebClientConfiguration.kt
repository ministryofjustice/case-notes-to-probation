package uk.gov.justice.digital.hmpps.pollpush.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfiguration(
  @Value("\${delius.endpoint.url}") private val communityApiRootUri: String,
  @Value("\${casenotes.endpoint.url}") private val caseNotesRootUri: String,
  @Value("\${oauth.endpoint.url}") private val oauthRootUri: String
) {

  @Bean
  fun authorizedWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient =
    ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
      .also { it.setDefaultClientRegistrationId("case-notes-to-probation") }
      .let { oauth2Client ->
        WebClient.builder()
          .apply(oauth2Client.oauth2Configuration())
          .build()
      }

  @Bean
  fun authorizedClientManager(
    clientRegistrationRepository: ClientRegistrationRepository?,
    oAuth2AuthorizedClientService: OAuth2AuthorizedClientService?
  ): OAuth2AuthorizedClientManager? =
    OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build()
      .let { provider ->
        AuthorizedClientServiceOAuth2AuthorizedClientManager(
          clientRegistrationRepository,
          oAuth2AuthorizedClientService
        ).apply { setAuthorizedClientProvider(provider) }
      }

  @Bean
  fun communityApiHealthWebClient(): WebClient = WebClient.create(communityApiRootUri)

  @Bean
  fun oauthApiHealthWebClient(): WebClient = WebClient.create(oauthRootUri)

  @Bean
  fun caseNotesApiHealthWebClient(): WebClient = WebClient.create(caseNotesRootUri)
}
