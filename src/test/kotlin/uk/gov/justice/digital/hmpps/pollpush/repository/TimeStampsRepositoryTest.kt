package uk.gov.justice.digital.hmpps.pollpush.repository


import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithAnonymousUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner

@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@WithAnonymousUser
open class TimeStampsRepositoryTest {

  @Autowired
  lateinit var timeStampsRepository: TimeStampsRepository

  @Test
  fun `should insert timestamp value`() {
    timeStampsRepository.save(TimeStamps("pull", "some pull value"))

    val (_, value) = timeStampsRepository.findById("pull").orElseThrow { RuntimeException("not found") }

    assertThat(value).isEqualTo("some pull value")
  }
}
