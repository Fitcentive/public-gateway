package io.fitcentive.public_gateway.controllers

import com.stripe.net.Webhook
import io.fitcentive.public_gateway.api.PaymentApi
import io.fitcentive.public_gateway.infrastructure.utils.ServerErrorHandler
import io.fitcentive.public_gateway.services.SettingsService
import io.fitcentive.sdk.play.{InternalAuthAction, UserAuthAction}
import io.fitcentive.sdk.utils.PlayControllerOps
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, PlayBodyParsers, RawBuffer, Request}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class PaymentController @Inject() (
  paymentApi: PaymentApi,
  userAuthAction: UserAuthAction,
  internalAuthAction: InternalAuthAction,
  cc: ControllerComponents,
  settingsService: SettingsService,
  parsers: PlayBodyParsers
)(implicit exec: ExecutionContext)
  extends AbstractController(cc)
  with PlayControllerOps
  with ServerErrorHandler {

  def stripeWebhook: Action[RawBuffer] =
    Action(parsers.raw).async { implicit request: Request[RawBuffer] =>
      val bodyOp = request.body.asBytes()
      val sigOp: Option[String] = request.headers.get("Stripe-Signature")
      (bodyOp, sigOp) match {
        case (Some(body), Some(signature)) =>
          Try {
            Webhook.constructEvent(body.utf8String, signature, settingsService.stripeConfig.webhookSecret)
          } match {
            case Failure(exception) => Future.successful(BadRequest(s"Invalid signature: $exception"))
            case Success(value) =>
              paymentApi
                .handleWebHookEvent(value)
                .map(_ => Ok)
                .recover(resultErrorAsyncHandler)
          }
        case _ => Future.successful(BadRequest)
      }
    }

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

  def deletePaymentMethod(p_id: String): Action[AnyContent] =
    userAuthAction.async { implicit userRequest =>
      paymentApi
        .deletePaymentMethod(userRequest.authorizedUser.userId, p_id)
        .map(_ => NoContent)
        .recover(resultErrorAsyncHandler)
    }

  def setPaymentAsSubscriptionDefault(p_id: String): Action[AnyContent] =
    userAuthAction.async { implicit userRequest =>
      paymentApi
        .setPaymentMethodAsDefaultForUserSubscriptions(userRequest.authorizedUser.userId, p_id)
        .map(_ => NoContent)
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

  // -------------------------
  // Internal routes
  // -------------------------

  def deleteUserData(userId: UUID): Action[AnyContent] =
    internalAuthAction.async { implicit userRequest =>
      paymentApi
        .deleteUserData(userId)
        .map(_ => NoContent)
        .recover(resultErrorAsyncHandler)
    }
}
