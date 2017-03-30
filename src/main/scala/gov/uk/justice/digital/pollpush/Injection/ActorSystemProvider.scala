package gov.uk.justice.digital.pollpush.Injection

import akka.actor.ActorSystem
import com.google.inject.Provider

class ActorSystemProvider extends Provider[ActorSystem] {

  override def get() = ActorSystem("system")
}
