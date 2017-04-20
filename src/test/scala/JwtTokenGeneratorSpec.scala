import gov.uk.justice.digital.pollpush.data.TokenPayload
import org.scalatest.{FunSpec, GivenWhenThen, Matchers}
import gov.uk.justice.digital.pollpush.services.JwtTokenGenerator
import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import org.json4s.native.Serialization._
import pdi.jwt.{Jwt, JwtAlgorithm}

class JwtTokenGeneratorSpec extends FunSpec with GivenWhenThen with Matchers {

  implicit val formats = Serialization.formats(NoTypeHints)

  describe("Token generator generates valid tokens") {

    it("Can decode a generated token") {

      val nomisToken = "abcde12345"
      val privateKey = "-----BEGIN PRIVATE KEY-----\nMIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgv6W6IfSP8OZZp3CU\n1CFl4xWHBMw0M5fLwJFkWyh0Ha6hRANCAATFcGap/UEOdNvsgUlJS5Qm9e6jclZo\n8qanO1ivSzKc4WzYObZNqIc1YwijC7z5B7z+ocH6zpNZRbpQe4jUiTCz\n-----END PRIVATE KEY-----"
      val publicKey = "-----BEGIN PUBLIC KEY-----\nMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAExXBmqf1BDnTb7IFJSUuUJvXuo3JW\naPKmpztYr0synOFs2Dm2TaiHNWMIowu8+Qe8/qHB+s6TWUW6UHuI1Ikwsw==\n-----END PUBLIC KEY-----"

      Given("a token generator")
      val tokenGenerator = new JwtTokenGenerator(privateKey, nomisToken)

      When("a token is generated")
      val token = tokenGenerator.generate()

      Then("it can be decoded into a recent payload")
      val decoded = Jwt.decode(token, publicKey, List(JwtAlgorithm.ES256)).get
      val payload = read[TokenPayload](decoded)

      payload.token shouldBe nomisToken
    }
  }
}
