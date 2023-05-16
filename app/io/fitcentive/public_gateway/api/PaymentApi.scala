package io.fitcentive.public_gateway.api

import com.stripe.model.Subscription
import io.fitcentive.public_gateway.domain.payment.{CustomerPaymentMethod, PaymentCustomer, PaymentSubscription}
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
      customerPaymentMethod <-
        customerRepository.upsertPaymentMethodForCustomer(userId, paymentCustomer.customerId, paymentMethodId)
      _ <- paymentService.attachPaymentMethodToCustomer(paymentMethodId, paymentCustomer.customerId)
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

  def getPremiumSubscriptions(userId: UUID): Future[Seq[PaymentSubscription]] =
    customerRepository.getSubscriptionsForUser(userId)

  def addPaymentMethod(userId: UUID, paymentMethodId: String): Future[CustomerPaymentMethod] =
    for {
      paymentCustomer <- getPaymentCustomer(userId)
      customerPaymentMethod <-
        customerRepository.upsertPaymentMethodForCustomer(userId, paymentCustomer.customerId, paymentMethodId)
    } yield customerPaymentMethod

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
