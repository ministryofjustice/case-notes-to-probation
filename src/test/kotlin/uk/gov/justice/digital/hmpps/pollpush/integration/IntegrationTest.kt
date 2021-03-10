package uk.gov.justice.digital.hmpps.pollpush.integration

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.pollpush.JwtAuthHelper
import uk.gov.justice.digital.hmpps.pollpush.services.AuthExtension
import uk.gov.justice.digital.hmpps.pollpush.services.CaseNotesExtension
import uk.gov.justice.digital.hmpps.pollpush.services.CommunityApiExtension
import uk.gov.justice.digital.hmpps.pollpush.services.QueueAdminService

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(CommunityApiExtension::class, AuthExtension::class, CaseNotesExtension::class)
abstract class IntegrationTest {
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  @SpyBean
  protected lateinit var queueAdminService: QueueAdminService

  init {
    SecurityContextHolder.getContext().authentication = TestingAuthenticationToken("user", "pw")
    // Resolves an issue where Wiremock keeps previous sockets open from other tests causing connection resets
    System.setProperty("http.keepAlive", "false")
  }

  internal fun setAuthorisation(
    user: String = "case-notes-to-probation-client",
    roles: List<String> = listOf()
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, roles)
}
