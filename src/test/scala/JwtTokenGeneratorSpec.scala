import gov.uk.justice.digital.pollpush.data.TokenPayload
import org.scalatest.{FunSpec, GivenWhenThen, Matchers}
import gov.uk.justice.digital.pollpush.services.JwtTokenGenerator
import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import org.json4s.native.Serialization._
import pdi.jwt.{Jwt, JwtAlgorithm}

import scala.io.Source

class JwtTokenGeneratorSpec extends FunSpec with GivenWhenThen with Matchers {

  implicit val formats = Serialization.formats(NoTypeHints)

  describe("Token generator generates valid tokens") {

    it("Can decode a generated token") {

      val nomisToken = "abcde12345"
      val privateKey = Source.fromResource("client.pkcs8.key").mkString
      val publicKey = Source.fromResource("client.pub").mkString

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
