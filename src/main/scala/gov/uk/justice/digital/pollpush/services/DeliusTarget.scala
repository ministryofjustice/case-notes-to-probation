package gov.uk.justice.digital.pollpush.services

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.google.inject.Inject
import com.google.inject.name.Named
import gov.uk.justice.digital.pollpush.data.{PushResult, TargetCaseNote}
import gov.uk.justice.digital.pollpush.traits.SingleTarget
import org.json4s.Formats
import org.json4s.native.Serialization._
import scala.concurrent.ExecutionContext.Implicits.global

class DeliusTarget @Inject() (@Named("targetUrl") targetUrl: String,
                              @Named("username") username: String,
                              @Named("password") password: String)
                             (implicit val formats: Formats,
                              implicit val system: ActorSystem,
                              implicit val materializer: ActorMaterializer) extends SingleTarget {

  private val http = Http()

  override def push(caseNote: TargetCaseNote) = {

    val request = HttpRequest(
      HttpMethods.PUT,
      Uri(s"$targetUrl/${caseNote.header}"),
      List(Authorization(BasicHttpCredentials(username, password))),
      HttpEntity(MediaTypes.`application/json`, write(caseNote.body))
    )

    http.singleRequest(request).flatMap { response =>

      for (body <- Unmarshal(response.entity).to[String])
        yield PushResult(caseNote, Some(response.status), body, None)

    }.recover {

      case t: Throwable => PushResult(caseNote, None, "", Some(t))
    }
  }
}
