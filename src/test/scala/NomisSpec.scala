import Helpers.SourceCaseNoteBuilder
import akka.actor.ActorSystem
import akka.http.scaladsl.model.DateTime
import akka.stream.ActorMaterializer
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import gov.uk.justice.digital.pollpush.data.{PullResult, SourceCaseNote}
import gov.uk.justice.digital.pollpush.services.NomisSource
import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import org.scalatest.{FunSpec, GivenWhenThen, Matchers}
import scala.concurrent.Await
import scala.concurrent.duration._

class NomisSpec extends FunSpec with GivenWhenThen with Matchers {

  implicit val formats = Serialization.formats(NoTypeHints)
  implicit val system = ActorSystem()
  implicit val materialzer = ActorMaterializer()

  describe("Pull from Nomis API") {

    it("GETs case notes from the API") {

      configureFor(8082)
      val api = new WireMockServer(options.port(8082))
      val source = new NomisSource("http://localhost:8082/nomisapi/offenders/events/case_notes")

      Given("the source API")
      api.start()
      val rightNow = DateTime.now
      val minuteAgo = rightNow.minus(6000)

      When("Case Notes are pulled from the API")
      val result = Await.result(source.pull(minuteAgo, rightNow), 5.seconds)

      Then("the API receives a HTTP GET call and returns the Case Notes")
      verify(getRequestedFor(urlEqualTo(s"/nomisapi/offenders/events/case_notes?from_datetime=${minuteAgo.toIsoDateTimeString}.000Z")))
      result shouldBe PullResult(Seq(
        SourceCaseNoteBuilder.build(
          "A1501AE",
          "152799",
          "OBSERVE",
          "Prisoner appears to have grown an extra arm ...[PHILL_GEN updated the case notes on 10-04-2017 14:57:26] Prisoner appears to have grown an extra arm and an extra leg",
          "2017-04-10T14:55:00.000Z",
          "Brady, Phill",
          "BMI"),
        SourceCaseNoteBuilder.build(
          "A1403AE",
          "152817",
          "ALERT",
          "Alert Sexual Offence and Risk to Children made active.",
          "2017-04-10T00:00:00.000Z",
          "Richardson, Trevor",
          "LEI")
      ), Some(minuteAgo), Some(rightNow), None)

      api.stop()
      system.terminate()
    }
  }
}
