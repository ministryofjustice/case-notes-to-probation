import Configuration.MockedConfiguration
import Helpers.SourceCaseNoteBuilder
import akka.actor.ActorSystem
import akka.http.scaladsl.model.{DateTime, StatusCodes}
import gov.uk.justice.digital.pollpush.Server
import gov.uk.justice.digital.pollpush.data._
import gov.uk.justice.digital.pollpush.traits.{BulkSource, DataStore, SingleTarget}
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfter, FunSpec, GivenWhenThen, Matchers}
import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global

class ServerSpec extends FunSpec with BeforeAndAfter with GivenWhenThen with Eventually with Matchers with BulkSource with SingleTarget with DataStore {

  describe("Case Note Processing") {

    it("pulls case notes from source, stores temporarily while pushing to target, then stores completion") {

      Given("the source system has two case notes")
      sourceNotes = twoCaseNotes

      When("the service runs")
      runServerWithMockedServices()

      Then("the two case notes are stored, copied to the target system, and deleted individually, then processed time is stored")
      eventually(fiveSecondTimeout) {

        targetNotes should (contain (Some(caseNote1.toTarget)) and contain(Some(caseNote2.toTarget)))
        storedNotes.length shouldBe 0
        maxStoredNotes shouldBe 2
        whenProcessed should not be None
      }
    }

    it("stores completion for an empty pull set") {

      Given("the source system has no case notes")
      sourceNotes = Seq()

      When("the service runs")
      runServerWithMockedServices()

      Then("no case notes are stored or pushed, but the processed time is still stored")
      eventually(fiveSecondTimeout) {

        targetNotes.length shouldBe 0
        storedNotes.length shouldBe 0
        maxStoredNotes shouldBe 0
        whenProcessed should not be None
      }
    }

    it("pulls source notes (two) from current datetime on startup if no lastProcessed is set, then sets lastProcessed") {

      Given("no lastProcessed is set and source system has two case notes")
      sourceNotes = twoCaseNotes
      whenProcessed = None

      When("the service runs")
      runServerWithMockedServices()

      Then("the source notes are pulled from Now, and the lastProcessed time is also set to Now")
      eventually(fiveSecondTimeout) {

        pullFromDateTime should not be None
        whenProcessed shouldBe pullUntilDateTime

        (DateTime.now.clicks - pullFromDateTime.get.clicks).toInt should be < 1000
      }
    }

    it("pulls source notes (empty) from current datetime on startup if no lastProcessed is set, then sets lastProcessed") {

      Given("no lastProcessed is set and source system has no case notes")
      whenProcessed = None

      When("the service runs")
      runServerWithMockedServices()

      Then("the source notes are pulled from Now, and the lastProcessed time is also set to Now")
      eventually(fiveSecondTimeout) {

        pullFromDateTime should not be None
        whenProcessed shouldBe pullUntilDateTime

        (DateTime.now.clicks - pullFromDateTime.get.clicks).toInt should be < 1000
      }
    }

    it("pulls source notes (two) from lastProcessed datetime on startup if set, then sets lastProcessed to Now") {

      Given("lastProcessed time was start of 2017 and source system has two case notes")
      sourceNotes = twoCaseNotes
      whenProcessed = startOf2017

      When("the service runs")
      runServerWithMockedServices()

      Then("the source notes are pulled from start of 2017, and the lastProcessed is set to Now")
      eventually(fiveSecondTimeout) {

        pullFromDateTime shouldBe startOf2017
        whenProcessed should not be startOf2017

        (DateTime.now.clicks - whenProcessed.get.clicks).toInt should be < 1000
      }
    }


    it("pulls source notes (empty) from lastProcessed datetime on startup if set, then sets lastProcessed to Now") {

      Given("lastProcessed time was start of 2017 and source system has no case notes")
      whenProcessed = startOf2017

      When("the service runs")
      runServerWithMockedServices()

      Then("the source notes are pulled from start of 2017, and the lastProcessed is set to Now")
      eventually(fiveSecondTimeout) {

        pullFromDateTime shouldBe startOf2017
        whenProcessed should not be startOf2017

        (DateTime.now.clicks - whenProcessed.get.clicks).toInt should be < 1000
      }
    }

    it("pulls source notes (two) multiple times over time") {

      Given("the source system has two case notes")
      sourceNotes = twoCaseNotes

      When("the service runs")
      runServerWithMockedServices()

      Then("the stored processed time changes over time")
      eventually(fiveSecondTimeout) {

        whenProcessed should not be None
      }

      val firstProcessed = whenProcessed

      eventually(tenSecondTimeout) {

        whenProcessed should not be firstProcessed
      }
    }

    it("pulls source notes (empty) multiple times over time") {

      Given("the source system has no case notes")
      sourceNotes = Seq()

      When("the service runs")
      runServerWithMockedServices()

      Then("the stored processed time changes over time")
      eventually(fiveSecondTimeout) {

        whenProcessed should not be None
      }

      val firstProcessed = whenProcessed

      eventually(tenSecondTimeout) {

        whenProcessed should not be firstProcessed
      }
    }

    it("a pull error results in retries of the same pull time period after timeout retries, and pullReceived is not set") {

      Given("a pull results in an error")
      pullErrorResponse = Option(new Exception("Pull Error"))

      When("the service runs")
      runServerWithMockedServices()

      Then("the same pull is attempted after the timeout")
      eventually(fiveSecondTimeout) {

        pullFromDateTime should not be None
      }

      val firstFromDateTime = pullFromDateTime

      pullFromDateTime = None // Set to None so can wait below for it to be set back to firstFromDateTime

      eventually(tenSecondTimeout) {

        pullFromDateTime shouldBe firstFromDateTime
      }

      pullFromDateTime = None // Set to None so can wait below for it to be set back to firstFromDateTime

      eventually(tenSecondTimeout) {

        pullFromDateTime shouldBe firstFromDateTime
      }

      whenReceived shouldBe None
    }

    it("processes recovered case notes if found on startup, but doesn't initially pull from source") {

      Given("recoverable notes are available on startup, and pullReceived has been previously set")
      storedNotes = twoCaseNotes.map(_.toTarget)
      whenReceived = startOf2017

      When("the service runs")
      runServerWithMockedServices()

      Then("the recovered notes are pushed to target, deleted from the store, lastProcessed is set, and no pull is performed ")
      eventually(fiveSecondTimeout) {

        targetNotes should (contain(Some(caseNote1.toTarget)) and contain(Some(caseNote2.toTarget)))
        storedNotes.length shouldBe 0
        whenProcessed shouldBe startOf2017
        pullFromDateTime shouldBe None
        pullUntilDateTime shouldBe None
      }
    }

    it("processed recovered case notes if found on startup, and starts pulling source again after a delay") {

      Given("recoverable notes are available on startup")
      storedNotes = twoCaseNotes.map(_.toTarget)

      When("the service runs")
      runServerWithMockedServices()

      Then("the recovered notes are pushed to target, and a pull from source is performed after a delay")
      eventually(fiveSecondTimeout) {

        targetNotes.length shouldBe 2
        storedNotes.length shouldBe 0
        pullFromDateTime shouldBe None
        pullUntilDateTime shouldBe None
      }

      eventually(tenSecondTimeout) {

        pullFromDateTime should not be None
        pullUntilDateTime should not be None
      }
    }

    it("stores pulled source case notes until the target push has completed") {

      Given("the source system has two case notes each pull, and pushing to the target system takes a long time")
      sourceNotes = twoCaseNotes
      pushImplementation = storeCaseNoteInfiniteWait

      When("the service runs")
      runServerWithMockedServices()

      Then("two case notes be initially pulled and stored while waiting for push to complete, then another two pulled and stored again")
      eventually(fiveSecondTimeout) {

        whenReceived should not be None
        storedNotes.length shouldBe 2
        targetNotes.length shouldBe 0
      }

      val firstReceived = whenReceived

      eventually(tenSecondTimeout) {

        whenReceived should not be firstReceived
        storedNotes.length shouldBe 4
        targetNotes.length shouldBe 0
      }
    }

    it("does not purge a pushed case note if pushing fails due to a runtime exceeded client thread pool and retries the push") {

      Given("pushing to the target system throws an runtime exception because the client thread pool is full")
      sourceNotes = twoCaseNotes
      pushImplementation = storeCaseNoteThrowsRuntimeExceptionExceeded

      When("the service runs")
      runServerWithMockedServices()

      Then("the two target notes are re-pushed 3 times each or more, and the notes remain stored in the database while re-pushing")

      eventually(fiveSecondTimeout) {

        targetNotes.length should be > 6
        storedNotes.length shouldBe 2
        whenReceived should not be None
      }
    }
  }

  private val caseNote1 = SourceCaseNoteBuilder.build("1234", "5678", "observation", "some notes", "time", "time", "Dave Smith", "XYZ")
  private val caseNote2 = SourceCaseNoteBuilder.build("5678", "9999", "regular", "more notes", "time", "time", "Johnny Jones", "ABA")

  private val twoCaseNotes = Seq(caseNote1, caseNote2)

  private val fiveSecondTimeout = Timeout(Span(5, Seconds))
  private val tenSecondTimeout = Timeout(Span(10, Seconds))

  private val startOf2017 = Some(DateTime.apply(2017, 1, 1))

  private var sourceNotes = Seq[SourceCaseNote]()
  private var storedNotes = Seq[TargetCaseNote]()
  private var targetNotes = Seq[Option[TargetCaseNote]]()
  private var maxStoredNotes = 0
  private var whenReceived: Option[DateTime] = None
  private var whenProcessed: Option[DateTime] = None
  private var runningService: Option[ActorSystem] = None
  private var pullFromDateTime: Option[DateTime] = None
  private var pullUntilDateTime: Option[DateTime] = None
  private var pullErrorResponse: Option[Throwable] = None

  private var pushImplementation: TargetCaseNote => Future[PushResult] = storeCaseNoteInTargetNotes

  before {
    sourceNotes = Seq()
    storedNotes = Seq()
    targetNotes = Seq()
    maxStoredNotes = 0
    whenReceived = None
    whenProcessed = None
    pullFromDateTime = None
    pullUntilDateTime = None
    pullErrorResponse = None
    pushImplementation = storeCaseNoteInTargetNotes
  }

  after {
    Thread.sleep(1000)              // Allow a second for all logging messages to be delivered
    runningService.get.terminate()
  }

  private def runServerWithMockedServices() = runningService = Some(Server.run(new MockedConfiguration(this, this, this, 5)))

  private def storeCaseNoteInTargetNotes(caseNote: TargetCaseNote) = {

    targetNotes = targetNotes :+ Some(caseNote)

    Future { PushResult(caseNote, Some(StatusCodes.NoContent), "", None) }
  }

  private def storeCaseNoteInfiniteWait(caseNote: TargetCaseNote) = Promise[PushResult]().future

  private def storeCaseNoteThrowsRuntimeExceptionExceeded(caseNote: TargetCaseNote) = {

    targetNotes = targetNotes :+ Some(caseNote)

    Future { PushResult(caseNote, Some(StatusCodes.NoContent), "", Some(new RuntimeException("Exceeded configured max-open-requests value of"))) }
  }

  override def pull(from: DateTime, until: DateTime) = {

    pullFromDateTime = Some(from)
    pullUntilDateTime = Some(until)

    val result = sourceNotes

    Future { PullResult(result, Some(from), Some(until), pullErrorResponse) }
  }

  override def push(caseNote: TargetCaseNote) = pushImplementation(caseNote) // Defaults to storeCaseNoteInTargetNotes

  override def save(caseNote: TargetCaseNote) = {

    storedNotes = storedNotes :+ caseNote
    maxStoredNotes = maxStoredNotes max storedNotes.length

    Future { SaveResult(caseNote, None) }
  }

  override def delete(caseNote: TargetCaseNote) = {

    storedNotes = storedNotes.filterNot(_.equals(caseNote))

    Future { DeleteResult(caseNote, None) }
  }

  override def count = {

    val result = storedNotes.length

    Future { CountResult(result, None) }
  }

  override def allCaseNotes = {

    val result = storedNotes

    Future { DataResult(result, None) }
  }

  override def pullReceived(dateTime: DateTime) = {

    whenReceived = Some(dateTime)

    Future { EmptyResult(None) }
  }

  override def pullProcessed() = {

    whenProcessed = whenReceived

    Future { EmptyResult(None) }
  }

  override def lastProcessedPull = {

    val result = whenProcessed

    Future { LastResult(result, None) }
  }
}
