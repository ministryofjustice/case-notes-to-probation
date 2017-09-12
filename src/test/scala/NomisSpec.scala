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

    it("GETs case notes from the API (with no noteType whitelist filter)") {

      val testPort = 8082

      configureFor(testPort)
      val api = new WireMockServer(options.port(testPort))
      val source = new NomisSource(s"http://localhost:$testPort/nomisapi/offenders/events/case_notes", Seq(""), this, 0)

      Given("the source API")
      api.start()
      val rightNow = DateTime.now
      val minuteAgo = rightNow.minus(6000)

      When("Case Notes are pulled from the API")
      val result = Await.result(source.pull(minuteAgo, rightNow), 5.seconds)

      Then("the API receives a HTTP GET call with Authorization, from_datetime and returns the Case Notes")
      verify(
        getRequestedFor(
          urlEqualTo(s"/nomisapi/offenders/events/case_notes?from_datetime=${minuteAgo.toIsoDateTimeString}.000Z")).
          withHeader("Authorization", equalTo("Bearer FooBar"))
      )
      result shouldBe PullResult(Seq(
        SourceCaseNoteBuilder.build(
          "A1501AE",
          "152799",
          "Observations",
          "Prisoner appears to have grown an extra arm ...[PHILL_GEN updated the case notes on 10-04-2017 14:57:26] Prisoner appears to have grown an extra arm and an extra leg",
          "2017-04-10T13:55:00.000Z",
          "2017-04-10T13:55:00.000Z",
          "Brady, Phill",
          "BMI"),
        SourceCaseNoteBuilder.build(
          "A1403AE",
          "152817",
          "Alert",
          "Alert Sexual Offence and Risk to Children made active.",
          "2017-04-09T23:00:00.000Z",
          "2017-04-10T13:55:00.000Z",
          "Richardson, Trevor",
          "LEI"),
        SourceCaseNoteBuilder.build(
          "A1301AB",
          "152837",
          "General",
          "Des who?",
          "2017-04-12T09:28:00.000Z",
          "2017-04-10T13:55:00.000Z",
          "Brady, Phill",
          "BMI"),
        SourceCaseNoteBuilder.build(
          "A1479AE",
          "152838",
          "Training",
          "Allocated to  course \"Tunnelling for beginners\"",
          "2017-04-12T13:12:00.000Z",
          "2017-04-10T13:55:00.000Z",
          "Brady, Phill",
          "BMI")
      ), Some(minuteAgo), Some(rightNow), None)

      api.stop()
    }

    it("GETs case notes with a noteType whitelist filter (singular)") {

      val testPort = 8083

      configureFor(testPort)
      val api = new WireMockServer(options.port(testPort))
      val source = new NomisSource(s"http://localhost:$testPort/nomisapi/offenders/events/case_notes", Seq("regular"), this, 0)

      Given("the source API")
      api.start()
      val rightNow = DateTime.now
      val minuteAgo = rightNow.minus(6000)

      When("Case Notes are pulled from the API")
      val result = Await.result(source.pull(minuteAgo, rightNow), 5.seconds)

      Then("the API receives a HTTP GET call with Authorization, from_datetime, and noteType whitelist")
      verify(
        getRequestedFor(
          urlEqualTo(s"/nomisapi/offenders/events/case_notes?from_datetime=${minuteAgo.toIsoDateTimeString}.000Z&note_type=regular")).
          withHeader("Authorization", equalTo("Bearer FooBar"))
      )
    }

    it("GETs case notes with a noteType whitelist filter (multiple)") {

      val testPort = 8084

      configureFor(testPort)
      val api = new WireMockServer(options.port(testPort))
      val source = new NomisSource(s"http://localhost:$testPort/nomisapi/offenders/events/case_notes", Seq("scheduled", "observation"), this, 0)

      Given("the source API")
      api.start()
      val rightNow = DateTime.now
      val minuteAgo = rightNow.minus(6000)

      When("Case Notes are pulled from the API")
      val result = Await.result(source.pull(minuteAgo, rightNow), 5.seconds)

      Then("the API receives a HTTP GET call with Authorization, from_datetime, and noteType whitelist")
      verify(
        getRequestedFor(
          urlEqualTo(s"/nomisapi/offenders/events/case_notes?from_datetime=${minuteAgo.toIsoDateTimeString}.000Z&note_type=scheduled&note_type=observation")).
          withHeader("Authorization", equalTo("Bearer FooBar"))
      )
    }

    it("reports a failure HTTP response code as an error") {

      val testPort = 8085

      configureFor(testPort)
      val api = new WireMockServer(options.port(testPort))
      val source = new NomisSource(s"http://localhost:$testPort/internalError", Seq(), this, 0)

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
