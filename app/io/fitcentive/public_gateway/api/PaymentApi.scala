package io.fitcentive.public_gateway.api

import com.stripe.model.{Event, StripeObject, Subscription}
import io.fitcentive.public_gateway.domain.payment.{
  CustomerPaymentMethod,
  PaymentCustomer,
  ProtectedCreditCard,
  StripeSubscription
}
import io.fitcentive.public_gateway.repositories.{CustomerRepository, UserTrialRepository}
import io.fitcentive.public_gateway.services.{MessageBusService, PaymentService, UserService}
import io.fitcentive.sdk.logging.AppLogger

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class PaymentApi @Inject() (
  customerRepository: CustomerRepository,
  userTrialRepository: UserTrialRepository,
  paymentService: PaymentService,
  userService: UserService,
  messageBusService: MessageBusService,
)(implicit ec: ExecutionContext)
  extends AppLogger {

  /*
    https://stripe.com/docs/billing/subscriptions/webhooks
    Some relevant events include:
      customer.source.expiring
      customer.subscription.resumed
      customer.subscription.updated
      customer.subscription.deleted - fired when payment fails due to retry rules configured in Stripe dashboard.
      invoice.payment_failed
      invoice.updated
      payment_intent.succeeded
      payment_intent.payment_failed
   */
  def handleWebHookEvent(event: Event): Future[Unit] = {

    /**
      * Deletes subscription for user and publishes event to cancel user premium
      * Also removes all associated payment methods to user
      */
    def handleCustomerSubscriptionDeleted: Future[Unit] =
      for {
        subscription <- {
          // https://stripe.com/docs/webhooks/quickstart?lang=java
          val dataObjectDeserializer = event.getDataObjectDeserializer
          val stripeObject: Option[StripeObject] = {
            if (dataObjectDeserializer.getObject.isPresent) Some(dataObjectDeserializer.getObject.get())
            else None
          }
          stripeObject
            .map(s => Future.successful(s.asInstanceOf[Subscription]))
            .getOrElse(Future.failed(new Exception("Unable to deserialize event data object")))
        }
        paymentCustomer <-
          customerRepository
            .getUserIdByCustomerId(subscription.getCustomer)
            .flatMap(_.map(Future.successful).getOrElse {
              logError("Error processing event customer.subscription.deleted: Unable to get userId from customerId")
              Future.failed(new Exception("Payment customer not found"))
            })
        _ <- customerRepository.deleteSubscriptionForUser(paymentCustomer.userId, subscription.getId)
        paymentMethods <- customerRepository.getPaymentMethodsForCustomer(paymentCustomer.userId)
        _ <- Future.sequence(
          paymentMethods.map(pm => paymentService.removePaymentMethodFromCustomer(pm.paymentMethodId, pm.customerId))
        )
        _ <- Future.sequence(
          paymentMethods
            .map(pm => customerRepository.deletePaymentMethodForCustomer(paymentCustomer.userId, pm.paymentMethodId))
        )
        _ <- messageBusService.publishDisablePremiumForUser(paymentCustomer.userId)
      } yield ()

    // todo - complete this
    def handleCustomerPaymentMethodExpiring: Future[Unit] =
      Future.unit

    logInfo(s"Stripe webhook - processing event: ${event.getType}")
    event.getType match {
      case "customer.source.expiring"      => handleCustomerPaymentMethodExpiring
      case "customer.subscription.deleted" => handleCustomerSubscriptionDeleted
      case unexpected                      => Future.unit
    }
  }

  // todo - db scheduler to disable premium after 32 days unless something changes with webhooks??
  def createPremiumSubscriptionForCustomer(userId: UUID, paymentMethodId: String): Future[Subscription] =
    for {
      paymentCustomer <- getPaymentCustomer(userId)
      _ <- paymentService.attachPaymentMethodToCustomer(paymentMethodId, paymentCustomer.customerId)
      hasUserAlreadyUsedTrial <- userTrialRepository.hasUserAlreadyEnabledTrial(userId)
      subscription <-
        paymentService.createSubscription(paymentCustomer.customerId, paymentMethodId, !hasUserAlreadyUsedTrial)
      _ <- customerRepository.createSubscriptionForUser(
        id = UUID.randomUUID(),
        userId = userId,
        subscriptionId = subscription.getId,
        customerId = paymentCustomer.customerId,
        isActive = true,
        validUntil = Instant.now.plus(32, ChronoUnit.DAYS)
      )
      _ <- userTrialRepository.upsertUserTrialStatus(userId, hasBeenUsed = true)
      _ <- paymentService.setPaymentMethodAsDefaultForCustomer(paymentMethodId, paymentCustomer.customerId)
      _ <- customerRepository.addPaymentMethodForCustomer(
        UUID.randomUUID(),
        userId,
        paymentCustomer.customerId,
        paymentMethodId,
        isDefault = true,
      )
      _ <- messageBusService.publishEnablePremiumForUser(userId)
    } yield subscription

  /*
  NOTE - we are not using the recoverWith block for above, as declined payment methods do not seem to attach to the customer_id to begin with
         This saves us a false positive failure of the type -
            "The payment method you provided is not attached to a customer so detachment is impossible"
  ).recoverWith {
      case e: StripeException =>
        logError(s"StripeException: ${e.getMessage}")
        getPaymentCustomer(userId)
          .flatMap(
            paymentCustomer =>
              paymentService.removePaymentMethodFromCustomer(paymentMethodId, paymentCustomer.customerId)
          )
          .flatMap(_ => Future.failed(e))
    }
   */

  // todo - we currently have at most 1 sub per user, do we need to enforce it?
  // When a user cancels premium, we also delete all previously saved user payment methods
  def cancelPremiumSubscriptionForCustomer(userId: UUID): Future[Unit] =
    for {
      subscriptions <- customerRepository.getSubscriptionsForUser(userId)
      _ <- Future.sequence(
        subscriptions.map(_.subscriptionId).map(subscriptionId => paymentService.cancelSubscription(subscriptionId))
      )
      _ <- Future.sequence(
        subscriptions
          .map(_.subscriptionId)
          .map(subscriptionId => customerRepository.deleteSubscriptionForUser(userId, subscriptionId))
      )
      paymentMethods <- customerRepository.getPaymentMethodsForCustomer(userId)
      _ <- Future.sequence(
        paymentMethods.map(pm => paymentService.removePaymentMethodFromCustomer(pm.paymentMethodId, pm.customerId))
      )
      _ <- Future.sequence(
        paymentMethods.map(pm => customerRepository.deletePaymentMethodForCustomer(userId, pm.paymentMethodId))
      )
      _ <- messageBusService.publishDisablePremiumForUser(userId)
    } yield ()

  def getPremiumSubscriptions(userId: UUID): Future[Seq[StripeSubscription]] =
    for {
      subs <- customerRepository.getSubscriptionsForUser(userId)
      stripeSubscriptions <- Future.sequence(subs.map(s => paymentService.getSubscription(s.subscriptionId)))
    } yield stripeSubscriptions.zip(subs).map {
      case (stripeSub, sub) =>
        StripeSubscription(
          id = sub.id,
          userId = sub.userId,
          subscriptionId = sub.subscriptionId,
          customerId = sub.customerId,
          startedAt = Instant.ofEpochSecond(stripeSub.getCurrentPeriodStart),
          validUntil = Instant.ofEpochSecond(stripeSub.getCurrentPeriodEnd),
          trialEnd = Try(stripeSub.getTrialEnd).map(s => Some(Instant.ofEpochSecond(s))).getOrElse(None),
          createdAt = sub.createdAt,
          updatedAt = sub.updatedAt,
        )
    }

  def getProtectedCardDetailsForUser(userId: UUID): Future[Seq[ProtectedCreditCard]] =
    for {
      paymentMethods <- customerRepository.getPaymentMethodsForCustomer(userId)
      protectedCards <- Future.sequence(
        paymentMethods.map(
          m =>
            paymentService
              .getProtectedCardInfo(m.paymentMethodId)
              .map(
                pm =>
                  ProtectedCreditCard(
                    lastFour = pm.getCard.getLast4,
                    expiryMonth = pm.getCard.getExpMonth,
                    expiryYear = pm.getCard.getExpYear,
                    isDefault = m.isDefault,
                    paymentMethodId = m.paymentMethodId,
                  )
              )
        )
      )
    } yield protectedCards

  def addPaymentMethod(userId: UUID, paymentMethodId: String): Future[CustomerPaymentMethod] =
    for {
      paymentCustomer <- getPaymentCustomer(userId)
      _ <- paymentService.attachPaymentMethodToCustomer(paymentMethodId, paymentCustomer.customerId)
      customerPaymentMethod <- customerRepository.addPaymentMethodForCustomer(
        UUID.randomUUID(),
        userId,
        paymentCustomer.customerId,
        paymentMethodId,
        isDefault = false,
      )
    } yield customerPaymentMethod

  def deletePaymentMethod(userId: UUID, paymentMethodIdToRemove: String): Future[Unit] =
    for {
      paymentCustomer <- getPaymentCustomer(userId)
      _ <- paymentService.removePaymentMethodFromCustomer(paymentMethodIdToRemove, paymentCustomer.customerId)
      _ <- customerRepository.deletePaymentMethodForCustomer(userId, paymentMethodIdToRemove)
      customerPaymentMethods <- customerRepository.getPaymentMethodsForCustomer(userId)
      _ <- Future.sequence(
        customerPaymentMethods
          .map(pm => customerRepository.setPaymentMethodAsNonDefaultForCustomer(userId, pm.paymentMethodId))
      )
      subscriptions <- customerRepository.getSubscriptionsForUser(userId)
      subscriptionDefaultPaymentMethods <-
        Future.sequence(subscriptions.map(s => paymentService.getPaymentMethodForSubscription(s.subscriptionId)))
      _ <- {
        if (customerPaymentMethods.nonEmpty) {
          val newDefaultPaymentId = customerPaymentMethods.head.paymentMethodId
          for {
            _ <- customerRepository.setPaymentMethodAsDefaultForCustomer(userId, newDefaultPaymentId)
            _ <- paymentService.setPaymentMethodAsDefaultForCustomer(newDefaultPaymentId, paymentCustomer.customerId)
            // Change defaults for subscription if needed - this is required in case the default payment method is being removed
            _ <- Future.sequence(subscriptions.zip(subscriptionDefaultPaymentMethods).map {
              case (subscription, defaultPaymentMethod) =>
                if (defaultPaymentMethod == paymentMethodIdToRemove)
                  paymentService
                    .setPaymentMethodAsDefaultForSubscription(newDefaultPaymentId, subscription.subscriptionId)
                else
                  Future.unit
            })
          } yield ()
        } else Future.unit
      }
    } yield ()

  def setPaymentMethodAsDefaultForUserSubscriptions(userId: UUID, paymentMethodId: String): Future[Unit] =
    for {
      paymentCustomer <- getPaymentCustomer(userId)
      _ <- paymentService.setPaymentMethodAsDefaultForCustomer(paymentMethodId, paymentCustomer.customerId)
      subscriptions <- customerRepository.getSubscriptionsForUser(userId)
      _ <- Future.sequence(
        subscriptions
          .map(s => paymentService.setPaymentMethodAsDefaultForSubscription(paymentMethodId, s.subscriptionId))
      )
      customerPaymentMethods <- customerRepository.getPaymentMethodsForCustomer(userId)
      _ <- Future.sequence(
        customerPaymentMethods
          .map(pm => customerRepository.setPaymentMethodAsNonDefaultForCustomer(userId, pm.paymentMethodId))
      )
      _ <- customerRepository.setPaymentMethodAsDefaultForCustomer(userId, paymentMethodId)
    } yield ()

  private def getPaymentCustomer(userId: UUID): Future[PaymentCustomer] =
    for {
      customerOpt <- customerRepository.getCustomerByUserId(userId)
      paymentCustomer <- customerOpt.fold {
        for {
          userDetails <-
            userService
              .getUser(userId)
              .flatMap(_.map(Future.successful).getOrElse(Future.failed(new Exception(""))))
          stripeCustomer <- paymentService.createCustomer(userDetails)
          newCust <- customerRepository.addCustomer(userId, stripeCustomer.getId)
        } yield newCust
      }(Future.successful)
    } yield paymentCustomer

  def deleteUserData(userId: UUID): Future[Unit] =
    for {
      _ <- cancelPremiumSubscriptionForCustomer(userId)
      _ <- customerRepository.deleteCustomerByUserId(userId)
      _ <- userTrialRepository.deleteUserTrialStatus(userId)
    } yield ()

}
