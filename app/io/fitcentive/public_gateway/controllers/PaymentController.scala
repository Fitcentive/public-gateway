package io.fitcentive.public_gateway.controllers

import io.fitcentive.public_gateway.api.PaymentApi
import io.fitcentive.public_gateway.infrastructure.utils.ServerErrorHandler
import io.fitcentive.sdk.play.UserAuthAction
import io.fitcentive.sdk.utils.PlayControllerOps
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class PaymentController @Inject() (paymentApi: PaymentApi, userAuthAction: UserAuthAction, cc: ControllerComponents)(
  implicit exec: ExecutionContext
) extends AbstractController(cc)
  with PlayControllerOps
  with ServerErrorHandler {

  def subscribeToPremium(p_id: String): Action[AnyContent] =
    userAuthAction.async { implicit userRequest =>
      paymentApi
        .createPremiumSubscriptionForCustomer(userRequest.authorizedUser.userId, p_id)
        .map(subscription => Ok(Json.toJson(subscription.getId)))
        .recover(resultErrorAsyncHandler)
    }

}
