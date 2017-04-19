import akka.http.scaladsl.model.DateTime
import gov.uk.justice.digital.pollpush.data.TokenPayload
import org.scalatest.{FunSpec, FunSuite, GivenWhenThen, Matchers}
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
      val privateKey = "MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQg0I/O+oZH/OAQVJHB8dvAD7gBMRUtwsFW75y7p1aflKSgCgYIKoZIzj0DAQehRANCAASAgCGtL4MyJc5xGfgIY/UP6EfHH09MsvOWeWL3tyod0QVia1yTrTQycjUc9sbgHZGxQJOi2fI6CBWZNBZ/MZso"
      val publicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEgIAhrS+DMiXOcRn4CGP1D+hHxx9PTLLzlnli97cqHdEFYmtck600MnI1HPbG4B2RsUCTotnyOggVmTQWfzGbKA=="

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
