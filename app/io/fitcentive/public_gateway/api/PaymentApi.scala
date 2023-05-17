package io.fitcentive.public_gateway.api

import com.stripe.model.Subscription
import io.fitcentive.public_gateway.domain.payment.{
  CustomerPaymentMethod,
  PaymentCustomer,
  ProtectedCreditCard,
  StripeSubscription
}
import io.fitcentive.public_gateway.repositories.CustomerRepository
import io.fitcentive.public_gateway.services.{MessageBusService, PaymentService, UserService}

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentApi @Inject() (
  customerRepository: CustomerRepository,
  paymentService: PaymentService,
  userService: UserService,
  messageBusService: MessageBusService,
)(implicit ec: ExecutionContext) {

  // todo - webhooks??
  // todo - db scheduler to disable premium after 32 days unless something changes with webhooks??
  def createPremiumSubscriptionForCustomer(userId: UUID, paymentMethodId: String): Future[Subscription] =
    for {
      paymentCustomer <- getPaymentCustomer(userId)
      customerPaymentMethod <- customerRepository.addPaymentMethodForCustomer(
        UUID.randomUUID(),
        userId,
        paymentCustomer.customerId,
        paymentMethodId,
        isDefault = true,
      )
      _ <- paymentService.attachPaymentMethodToCustomer(paymentMethodId, paymentCustomer.customerId)
      _ <-
        paymentService.setPaymentMethodAsDefaultForSubscriptionForCustomer(paymentMethodId, paymentCustomer.customerId)
      subscription <- paymentService.createSubscription(paymentCustomer.customerId, paymentMethodId)
      _ <- customerRepository.createSubscriptionForUser(
        id = UUID.randomUUID(),
        userId = userId,
        subscriptionId = subscription.getId,
        customerId = paymentCustomer.customerId,
        isActive = true,
        validUntil = Instant.now.plus(32, ChronoUnit.DAYS)
      )
      _ <- messageBusService.publishEnablePremiumForUser(userId)
    } yield subscription

  // todo - we currently have at most 1 sub per user, do we need to enforce it?
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

  def deletePaymentMethod(userId: UUID, paymentMethodId: String): Future[Unit] =
    for {
      paymentCustomer <- getPaymentCustomer(userId)
      _ <- paymentService.removePaymentMethodFromCustomer(paymentMethodId, paymentCustomer.customerId)
      _ <- customerRepository.deletePaymentMethodForCustomer(userId, paymentMethodId)
      customerPaymentMethods <- customerRepository.getPaymentMethodsForCustomer(userId)
      _ <- Future.sequence(
        customerPaymentMethods
          .map(pm => customerRepository.setPaymentMethodAsNonDefaultForCustomer(userId, pm.paymentMethodId))
      )
      _ <- {
        if (customerPaymentMethods.nonEmpty)
          for {
            _ <- customerRepository.setPaymentMethodAsDefaultForCustomer(
              userId,
              customerPaymentMethods.head.paymentMethodId
            )
            _ <- paymentService.setPaymentMethodAsDefaultForSubscriptionForCustomer(
              customerPaymentMethods.head.paymentMethodId,
              paymentCustomer.customerId
            )
          } yield ()
        else Future.unit
      }
    } yield ()

  def setPaymentMethodAsDefaultForUserSubscriptions(userId: UUID, paymentMethodId: String): Future[Unit] =
    for {
      paymentCustomer <- getPaymentCustomer(userId)
      _ <-
        paymentService.setPaymentMethodAsDefaultForSubscriptionForCustomer(paymentMethodId, paymentCustomer.customerId)
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

}
