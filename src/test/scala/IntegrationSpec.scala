import Configuration.{IntegrationConfiguration, MongoEmbedConfiguration}
import Helpers.MongoEmbedClient
import akka.actor.ActorSystem
import com.github.simplyscala.MongoEmbedDatabase
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import gov.uk.justice.digital.pollpush.Server
import gov.uk.justice.digital.pollpush.data.{TargetCaseNote, TargetCaseNoteBody, TargetCaseNoteHeader}
import org.scalatest.{BeforeAndAfter, FunSpec, GivenWhenThen, Matchers}
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Seconds, Span}
import scala.concurrent.Await
import scala.concurrent.duration._

class IntegrationSpec extends FunSpec with BeforeAndAfter with GivenWhenThen with Eventually with Matchers with MongoEmbedDatabase {

  describe("Full Integration tests") {

    it("pulls case notes batch from source, pushes to target in parallel, and records lastProcessed datetime") {

      withEmbedMongoFixture() { _ => // Fires up an embedded MongoDB on localhost:12345

        Given("the source system has four case notes")
        lastProcessed shouldBe None

        When("the case notes are received from source in around 2 seconds")
        runServerWithMongoEmbed("regular,other")

        Then("the case notes are pushed to target simultaneously within 1 to 3 seconds each, and lastProcessed is set")
        eventually(tenSecondTimeout) { // Allow 2 seconds to pull and max of 3 seconds to push simultaneously, plus start up time

          lastProcessed should not be None  // Check that lastProcessedPull has been set in the database after pull
          storedNotesTotal.number shouldBe 0

          verify(
            getRequestedFor(urlPathEqualTo("/nomisapi/offenders/events/case_notes")).
              withHeader("Authorization", containing("Bearer"))
          )

          verify(getRequestedFor(urlMatching(".*&note_type=regular&note_type=other.*")))

          verify(putRequestedFor(urlEqualTo("/delius/A1501AE/152799")))
          verify(putRequestedFor(urlEqualTo("/delius/A1403AE/152817")))
        }
      }
    }

    it("recovers case notes previously pulled from source but not yet pushed on startup and pushes to target") {

      withEmbedMongoFixture() { _ => // Fires up an embedded MongoDB on localhost:12345

        Given("the MongoDB contains two recoverable case notes")
        saveCaseNote(caseNote1)
        saveCaseNote(caseNote2)

        When("the service runs")
        runServerWithMongoEmbed()

        Then("the recovered case notes are pushed to target and removed from MongoDB")
        eventually(fiveSecondTimeout) {

          storedNotesTotal.number shouldBe 0

          verify(putRequestedFor(urlEqualTo("/delius/2222/BBBB")))
          verify(putRequestedFor(urlEqualTo("/delius/3333/DDDD")))
        }
      }
    }
  }

  private val caseNote1 = TargetCaseNote(TargetCaseNoteHeader("2222", "BBBB"), TargetCaseNoteBody("Other", "Example note", "time", "time", "David Bowie", "CCC"))
  private val caseNote2 = TargetCaseNote(TargetCaseNoteHeader("3333", "DDDD"), TargetCaseNoteBody("Regular", "Kind og note", "time", "time", "David Brent", "FFF"))

  private val store = MongoEmbedClient.store()

  private val fiveSecondTimeout = Timeout(Span(5, Seconds))
  private val tenSecondTimeout = Timeout(Span(10, Seconds))

  private var mockedRestAPIs: Option[WireMockServer] = None
  private var runningService: Option[ActorSystem] = None

  before {
    mockedRestAPIs = Some(new WireMockServer())  // Mocks Nomis based on getCaseNotes.json, and Delius REST services on localhost:8080
    mockedRestAPIs.get.start()
  }

  after {
    mockedRestAPIs.get.stop()
    runningService.get.terminate()
  }

  private def runServerWithMongoEmbed(pullNoteTypes: String = "") = runningService = Some(Server.run(new IntegrationConfiguration(pullNoteTypes)))

  private def lastProcessed = Await.result(store.lastProcessedPull, 10.seconds).dateTime
  private def storedNotesTotal = Await.result(store.count, 10.seconds)
  private def saveCaseNote(caseNote: TargetCaseNote) = Await.result(store.save(caseNote), 10.seconds)
}
