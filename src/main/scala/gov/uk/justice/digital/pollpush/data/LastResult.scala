package gov.uk.justice.digital.pollpush.data

import akka.http.scaladsl.model.DateTime

case class LastResult(dateTime: Option[DateTime], error: Option[Throwable])
