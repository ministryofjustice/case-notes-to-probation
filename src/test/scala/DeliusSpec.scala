import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.BasicCredentials
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import gov.uk.justice.digital.pollpush.data.{PushResult, TargetCaseNote, TargetCaseNoteBody, TargetCaseNoteHeader}
import gov.uk.justice.digital.pollpush.services.DeliusTarget
import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import org.scalatest.{FunSpec, GivenWhenThen, Matchers}
import scala.concurrent.Await
import scala.concurrent.duration._

class DeliusSpec extends FunSpec with GivenWhenThen with Matchers {

  implicit val formats = Serialization.formats(NoTypeHints)
  implicit val system = ActorSystem()
  implicit val materialzer = ActorMaterializer()

  describe("Push to Delius API") {

    it("PUTs a case note into the API") {

      configureFor(8081)
      val api = new WireMockServer(options.port(8081))
      val target = new DeliusTarget("http://localhost:8081/delius", "username", "password")
      api.start()

      Given("a Case Note")
      val caseNote = TargetCaseNote(TargetCaseNoteHeader("5678", "efgh"), TargetCaseNoteBody("regular", "more notes", "time", "time", "John Smith", "XXX"))

      When("the Case Note is pushed to the target")
      val result = Await.result(target.push(caseNote), 5.seconds)
      system.terminate()

      Then("the API receives a HTTP PUT call")
      verify(
        putRequestedFor(urlEqualTo("/delius/5678/efgh")).
        withHeader("Content-type", equalTo("application/json")).
        withBasicAuth(new BasicCredentials("username", "password")).
        withRequestBody(equalTo("{\"noteType\":\"regular\",\"content\":\"more notes\",\"contactTimestamp\":\"time\",\"systemTimestamp\":\"time\",\"staffName\":\"John Smith\",\"establishmentCode\":\"XXX\"}"))
      )
      result shouldBe PushResult(caseNote, Some(StatusCodes.NoContent), "", None)

      api.stop()
    }
  }
}
