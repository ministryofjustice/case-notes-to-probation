import akka.http.scaladsl.model.StatusCodes
import gov.uk.justice.digital.pollpush.Server
import gov.uk.justice.digital.pollpush.data._
import gov.uk.justice.digital.pollpush.traits.{BulkSource, SingleTarget}
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{FunSpec, GivenWhenThen, Matchers}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ServerSpec extends FunSpec with GivenWhenThen with Eventually with Matchers with BulkSource with SingleTarget {

  val caseNote1 = SourceCaseNote("1234", "abcd", "observation", "some notes", "time")
  val caseNote2 = SourceCaseNote("5678", "efgh", "regular", "more notes", "time")

  private var receivedNotes = Seq[Option[TargetCaseNote]]()

  override def pull() = Future { PullResult(Seq(caseNote1, caseNote2), None) }

  override def push(caseNote: TargetCaseNote) = {

    receivedNotes = receivedNotes :+ Some(caseNote)

    Future { PushResult(caseNote, Some(StatusCodes.NoContent), "", None) }
  }

  describe("Case Note Processing") {

    it("pulls case notes from source and pushes to target") {

      Given("the source system has two case notes")
      val testConfig = new TestConfiguration(this, this)

      When("the service runs")
      val system = Server.run(testConfig)

      Then("the two case notes are copied to the target system one at a time")
      eventually(Timeout(Span(5, Seconds))) { receivedNotes should equal(Seq(Some(caseNote1.toTarget), Some(caseNote2.toTarget))) }

      Thread.sleep(1000) // Allow a second for PushResult logging messages to be delivered
      system.terminate()
    }
  }
}
