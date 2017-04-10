package gov.uk.justice.digital.pollpush.Injection

import com.google.inject.Provider
import reactivemongo.api.MongoDriver

class MongoDriverProvider extends Provider[MongoDriver] {

  override def get() = MongoDriver()
}
