import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.{putRequestedFor, urlEqualTo, verify}
import gov.uk.justice.digital.pollpush.Server
import org.scalatest.{FunSpec, GivenWhenThen, Matchers}
import org.scalatest.concurrent.Eventually

class IntegrationSpec  extends FunSpec with GivenWhenThen with Eventually with Matchers {

  describe("Full Integration test") {

    it("pulls case notes batch from source and pushes to target in parallel") {

      val api = new WireMockServer()

      Given("the source system has four case notes")
      api.start()

      When("the four case notes are received from source in around 2 seconds")
      val system = Server.run()

      Then("the four case notes are pushed simultaneously which takes a random amount of time each 1 to 3 seconds each")
      Thread.sleep(7500) // Allow 2 seconds to pull and max of 3 seconds to push simultaneously
      verify(putRequestedFor(urlEqualTo("/delius/1234/ABCD")))
      verify(putRequestedFor(urlEqualTo("/delius/5678/EFGH")))
      verify(putRequestedFor(urlEqualTo("/delius/9876/IJKL")))
      verify(putRequestedFor(urlEqualTo("/delius/5432/MNOP")))

      api.stop()
      system.terminate()
    }
  }
}
