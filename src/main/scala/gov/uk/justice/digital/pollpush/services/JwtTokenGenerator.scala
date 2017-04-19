package gov.uk.justice.digital.pollpush.services

import akka.http.scaladsl.model.DateTime
import com.google.inject.Inject
import com.google.inject.name.Named
import gov.uk.justice.digital.pollpush.data.TokenPayload
import gov.uk.justice.digital.pollpush.traits.SourceToken
import pdi.jwt.{Jwt, JwtAlgorithm}
import org.json4s.Formats
import org.json4s.native.Serialization._

class JwtTokenGenerator @Inject()(@Named("privateKey") privateKey: String,
                                  @Named("nomisToken") nomisToken: String)
                                  (implicit val formats: Formats) extends SourceToken {

  override def generate() = {
    privateKey match {
      case "" => ""
      case _ => Jwt.encode(write(TokenPayload(DateTime.now.clicks / 1000, nomisToken)), privateKey, JwtAlgorithm.ES256)
    }
  }
}
