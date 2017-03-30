package gov.uk.justice.digital.pollpush.services

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.ActorMaterializer
import com.google.inject.Inject
import com.google.inject.name.Named
import gov.uk.justice.digital.pollpush.data.PullResult
import gov.uk.justice.digital.pollpush.traits.BulkSource
import org.json4s.Formats
import org.json4s.native.Serialization._
import scala.concurrent.ExecutionContext.Implicits.global

class NomisSource @Inject() (@Named("sourceUrl") sourceUrl: String)
                            (implicit val formats: Formats,
                             implicit val system: ActorSystem,
                             implicit val materializer: ActorMaterializer) extends BulkSource {

  private val http = Http()

  implicit val unmarshaller = Unmarshaller.stringUnmarshaller.forContentTypes(MediaTypes.`application/json`).map(read[PullResult])

  override def pull() =

    http.singleRequest(HttpRequest(uri = Uri(sourceUrl))).flatMap { response =>

      Unmarshal(response.entity).to[PullResult]

    }.recover {

      case error: Throwable => PullResult(Seq(), Some(error))
    }
}
