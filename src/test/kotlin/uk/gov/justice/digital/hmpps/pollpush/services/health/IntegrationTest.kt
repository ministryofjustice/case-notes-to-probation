package uk.gov.justice.digital.hmpps.pollpush.services.health

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.pollpush.services.AuthExtension
import uk.gov.justice.digital.hmpps.pollpush.services.CaseNotesExtension
import uk.gov.justice.digital.hmpps.pollpush.services.CommunityApiExtension

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(CommunityApiExtension::class, AuthExtension::class, CaseNotesExtension::class)
abstract class IntegrationTest {
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  init {
    SecurityContextHolder.getContext().authentication = TestingAuthenticationToken("user", "pw")
    // Resolves an issue where Wiremock keeps previous sockets open from other tests causing connection resets
    System.setProperty("http.keepAlive", "false")
  }
}
