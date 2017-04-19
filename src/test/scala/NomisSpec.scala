import Helpers.SourceCaseNoteBuilder
import akka.actor.ActorSystem
import akka.http.scaladsl.model.DateTime
import akka.stream.ActorMaterializer
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import gov.uk.justice.digital.pollpush.data.PullResult
import gov.uk.justice.digital.pollpush.services.NomisSource
import gov.uk.justice.digital.pollpush.traits.SourceToken
import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import org.scalatest.{BeforeAndAfterAll, FunSpec, GivenWhenThen, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

class NomisSpec extends FunSpec with BeforeAndAfterAll with GivenWhenThen with Matchers with SourceToken {

  implicit val formats = Serialization.formats(NoTypeHints)
  implicit val system = ActorSystem()
  implicit val materialzer = ActorMaterializer()

  describe("Pull from Nomis API") {

    it("GETs case notes from the API") {

      configureFor(8082)
      val api = new WireMockServer(options.port(8082))
      val source = new NomisSource("http://localhost:8082/nomisapi/offenders/events/case_notes", this)

      Given("the source API")
      api.start()
      val rightNow = DateTime.now
      val minuteAgo = rightNow.minus(6000)

      When("Case Notes are pulled from the API")
      val result = Await.result(source.pull(minuteAgo, rightNow), 5.seconds)

      Then("the API receives a HTTP GET call with Authorization and returns the Case Notes")
      verify(
        getRequestedFor(
          urlEqualTo(s"/nomisapi/offenders/events/case_notes?from_datetime=${minuteAgo.toIsoDateTimeString}.000Z")).
          withHeader("Authorization", equalTo("Bearer FooBar"))
      )
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
    }

    it("reports a failure HTTP response code as an error") {

      configureFor(8083)
      val api = new WireMockServer(options.port(8083))
      val source = new NomisSource("http://localhost:8083/internalError", this)

      Given("the source API returns an 500 Internal Error")
      api.start()
      val rightNow = DateTime.now
      val minuteAgo = rightNow.minus(6000)

      When("a Case Notes pull from the API is attempted")
      val result = Await.result(source.pull(minuteAgo, rightNow), 5.seconds)

      Then("the 500 error is reported")
      result.error.get.toString should include("500")

      api.stop()
    }
  }

  override def afterAll() {

    system.terminate()
  }

  override def generate() = "FooBar"
}
