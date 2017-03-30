package gov.uk.justice.digital.pollpush.Injection

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.google.inject.{Inject, Provider}

class ActorMaterializerProvider @Inject() (implicit val system: ActorSystem) extends Provider[ActorMaterializer] {

  override def get() = ActorMaterializer()
}
