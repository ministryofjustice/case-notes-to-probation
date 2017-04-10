import Helpers.MongoEmbedClient
import akka.http.scaladsl.model.DateTime
import com.github.simplyscala.MongoEmbedDatabase
import gov.uk.justice.digital.pollpush.data.{TargetCaseNote, TargetCaseNoteBody, TargetCaseNoteHeader}
import org.scalatest.{FunSpec, GivenWhenThen, Matchers}
import scala.concurrent.Await
import scala.concurrent.duration._

class MongoSpec extends FunSpec with GivenWhenThen with Matchers with MongoEmbedDatabase {

  describe("A MongoDB store for Case Notes") {

    it("can store a case note") {

      withEmbedMongoFixture(12346) { _ => // Fires up an embedded MongoDB on localhost:12346

        val mongoStore = MongoEmbedClient.store(12346)

        Given("An empty store")
        Await.result(mongoStore.count, tenSeconds).number shouldBe 0

        When("a case note is saved in the store")
        Await.result(mongoStore.save(caseNote1), tenSeconds)

        Then("the store now contains one case note")
        Await.result(mongoStore.count, tenSeconds).number shouldBe 1
      }
    }

    it("can store and retrieve a case note") {

      withEmbedMongoFixture(12347) { _ => // Fires up an embedded MongoDB on localhost:12347

        val mongoStore = MongoEmbedClient.store(12347)

        Given("An empty store")
        Await.result(mongoStore.allCaseNotes, tenSeconds).casenotes shouldBe empty

        When("a case note is saved in the store")
        Await.result(mongoStore.save(caseNote1), tenSeconds)

        Then("the case note can be retrieved")
        Await.result(mongoStore.allCaseNotes, tenSeconds).casenotes.head.copy(id = None) shouldBe caseNote1
      }
    }

    it("can store and delete a case note") {

      withEmbedMongoFixture(12348) { _ => // Fires up an embedded MongoDB on localhost:12348

        val mongoStore = MongoEmbedClient.store(12348)

        Given("A stored case note")
        val storedNote = Await.result(mongoStore.save(caseNote1), tenSeconds).caseNote

        When("the case note is deleted from the store")
        Await.result(mongoStore.delete(storedNote), tenSeconds)

        Then("the store is empty")
        Await.result(mongoStore.count, tenSeconds).number shouldBe 0
      }
    }

    it("can record and retrieve the last processed date") {

      withEmbedMongoFixture(12349) { _ => // Fires up an embedded MongoDB on localhost:12349

        val mongoStore = MongoEmbedClient.store(12349)

        Given("a received pull has been recorded")
        Await.result(mongoStore.pullReceived(startOf2017), tenSeconds)

        When("the pull is recorded as processed")
        Await.result(mongoStore.pullProcessed(), tenSeconds)

        Then("the last processed pull date time can be retrieved")
        Await.result(mongoStore.lastProcessedPull, tenSeconds).dateTime.get shouldBe startOf2017
      }
    }
  }

  private val caseNote1 = TargetCaseNote(TargetCaseNoteHeader("4444", "GGGG"), TargetCaseNoteBody("Normal", "A case note", "time", "David Essex", "HHH"))
  private val tenSeconds = 10.seconds
  private val startOf2017 = DateTime.apply(2017, 1, 1)
}
