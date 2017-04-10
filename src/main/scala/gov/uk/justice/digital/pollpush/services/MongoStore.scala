package gov.uk.justice.digital.pollpush.services

import akka.http.scaladsl.model.DateTime
import com.google.inject.Inject
import com.google.inject.name.Named
import gov.uk.justice.digital.pollpush.data._
import gov.uk.justice.digital.pollpush.traits.DataStore
import reactivemongo.api.{Cursor, MongoConnection}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson._
import scala.language.implicitConversions
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MongoStore @Inject() (connection: MongoConnection, @Named("dbName") dbName: String) extends DataStore {

  private implicit val writerHelper1 = Macros.writer[TargetCaseNoteHeader]
  private implicit val writerHelper2 = Macros.writer[TargetCaseNoteBody]
  private implicit val readerHelper1 = Macros.reader[TargetCaseNoteHeader]
  private implicit val readerHelper2 = Macros.reader[TargetCaseNoteBody]

  private val caseNoteWriter = Macros.writer[TargetCaseNote]
  private val caseNoteReader = Macros.reader[TargetCaseNote]

  private val pullReceivedQuery = BSONDocument("_id" -> "pullReceived")
  private val pullProcessedQuery = BSONDocument("_id" -> "pullProcessed")

  implicit object TargetCaseNoteReader extends BSONDocumentReader[TargetCaseNote] {

    override def read(bson: BSONDocument) = caseNoteReader.read(bson).copy(id = bson.getAs[BSONObjectID]("_id").map(_.stringify))
  }

  private def database = connection.database(dbName)

  private def collection(name: String) = database.map(_.collection[BSONCollection](name))

  private def caseNotes = collection("caseNotes")
  private def timeStamps = collection("timeStamps")


  private implicit def resultToException(result: WriteResult): Option[Throwable] =

    if (result.ok) None else Some(new Exception(s"Write Failed: ${result.code.getOrElse(0)}")) // used in xxxxxxResult


  override def save(caseNote: TargetCaseNote) = caseNotes.flatMap { collection =>

    val id = BSONObjectID.generate()

    for (result <- collection.insert(caseNoteWriter.write(caseNote).merge("_id" -> id)))
      yield SaveResult(caseNote.copy(id = Some(id.stringify)), result)

  }.recover { case error: Throwable => SaveResult(caseNote, Some(error)) }


  override def delete(caseNote: TargetCaseNote) = (caseNote match { case TargetCaseNote(_, _, Some(id)) =>

    caseNotes.flatMap { collection =>

      collection.remove(BSONDocument("_id" -> BSONObjectID.parse(id).toOption.get)).map(DeleteResult(caseNote, _))
    }
  }).recover { case error: Throwable => DeleteResult(caseNote, Some(error)) }


  override def count = caseNotes.flatMap(_.count().map(CountResult(_, None))).recover {

    case error: Throwable => CountResult(0, Some(error))
  }


  override def allCaseNotes = caseNotes.flatMap { collection =>

    for (result <- collection.find(BSONDocument()).cursor[TargetCaseNote]().collect[List](Int.MaxValue, Cursor.FailOnError[List[TargetCaseNote]]()))
      yield DataResult(result, None)

  }.recover { case error: Throwable => DataResult(Seq(), Some(error)) }


  override def pullReceived(dateTime: DateTime) = timeStamps.flatMap { collection =>

    collection.update(pullReceivedQuery, BSONDocument("value" -> dateTime.toString), upsert = true).map(EmptyResult(_))

  }.recover { case error: Throwable => EmptyResult(Some(error)) }


  override def pullProcessed() = timeStamps.flatMap { collection =>

    collection.find(pullReceivedQuery).one[BSONDocument].map(_.flatMap(_.getAs[BSONString]("value"))).flatMap {

      case None => Future { EmptyResult(None) } // Can only occur if pullProcessed() called before pullReceived() which shouldn't happen
      case Some(dateTimeString) =>

        collection.update(pullProcessedQuery, BSONDocument("value" -> dateTimeString), upsert = true).map(EmptyResult(_))
    }
  }.recover { case error: Throwable => EmptyResult(Some(error)) }


  override def lastProcessedPull = timeStamps.flatMap { collection =>

    for (result <- collection.find(pullProcessedQuery).one[BSONDocument])
      yield LastResult(result.map(_.getAs[BSONString]("value").get.value).flatMap(DateTime.fromIsoDateTimeString), None)

  }.recover { case error: Throwable => LastResult(None, Some(error)) }

}
