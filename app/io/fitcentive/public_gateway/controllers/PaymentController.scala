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

  def getPaymentMethods: Action[AnyContent] =
    userAuthAction.async { implicit userRequest =>
      paymentApi
        .getProtectedCardDetailsForUser(userRequest.authorizedUser.userId)
        .map(paymentMethods => Ok(Json.toJson(paymentMethods)))
        .recover(resultErrorAsyncHandler)
    }

  def addPaymentMethod(p_id: String): Action[AnyContent] =
    userAuthAction.async { implicit userRequest =>
      paymentApi
        .addPaymentMethod(userRequest.authorizedUser.userId, p_id)
        .map(paymentMethod => Ok(Json.toJson(paymentMethod)))
        .recover(resultErrorAsyncHandler)
    }

  def subscribeToPremium(p_id: String): Action[AnyContent] =
    userAuthAction.async { implicit userRequest =>
      paymentApi
        .createPremiumSubscriptionForCustomer(userRequest.authorizedUser.userId, p_id)
        .map(subscription => Ok(Json.toJson(subscription.getId)))
        .recover(resultErrorAsyncHandler)
    }

  def cancelPremium: Action[AnyContent] =
    userAuthAction.async { implicit userRequest =>
      paymentApi
        .cancelPremiumSubscriptionForCustomer(userRequest.authorizedUser.userId)
        .map(_ => Accepted)
        .recover(resultErrorAsyncHandler)
    }

  def getPremiumSubscriptions: Action[AnyContent] =
    userAuthAction.async { implicit userRequest =>
      paymentApi
        .getPremiumSubscriptions(userRequest.authorizedUser.userId)
        .map(subscriptions => Ok(Json.toJson(subscriptions)))
        .recover(resultErrorAsyncHandler)
    }

}
