package io.fitcentive.public_gateway.infrastructure.utils

import com.stripe.exception.StripeException
import io.fitcentive.public_gateway.domain.errors.ImageUploadError
import io.fitcentive.sdk.error.DomainError
import io.fitcentive.sdk.logging.AppLogger
import io.fitcentive.sdk.utils.DomainErrorHandler
import play.api.mvc.Result
import play.api.mvc.Results._

trait ServerErrorHandler extends DomainErrorHandler with AppLogger {

  override def resultErrorAsyncHandler: PartialFunction[Throwable, Result] = {
    case e: StripeException =>
      logError(s"${e.getMessage}", e)
      BadRequest(e.getMessage)

    case e: Exception =>
      logError(s"${e.getMessage}", e)
      InternalServerError(e.getMessage)
  }

  override def domainErrorHandler: PartialFunction[DomainError, Result] = {
    case ImageUploadError(reason) => InternalServerError(reason)
    case _                        => InternalServerError("Unexpected error occurred ")
  }

}
