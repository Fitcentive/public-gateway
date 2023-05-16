package io.fitcentive.public_gateway.repositories

import com.google.inject.ImplementedBy
import io.fitcentive.public_gateway.domain.payment.{CustomerPaymentMethod, PaymentCustomer, PaymentSubscription}
import io.fitcentive.public_gateway.infrastructure.database.sql.AnormStripeCustomerRepository

import java.time.Instant
import java.util.UUID
import scala.concurrent.Future

@ImplementedBy(classOf[AnormStripeCustomerRepository])
trait CustomerRepository {
  def getCustomerByUserId(userId: UUID): Future[Option[PaymentCustomer]]
  def addCustomer(userId: UUID, customerId: String): Future[PaymentCustomer]
  def getSubscriptionsForUser(userId: UUID): Future[Seq[PaymentSubscription]]
  def createSubscriptionForUser(
    id: UUID,
    userId: UUID,
    subscriptionId: String,
    customerId: String,
    isActive: Boolean,
    validUntil: Instant
  ): Future[PaymentSubscription]
  def deleteSubscriptionForUser(userId: UUID, subscriptionId: String): Future[Unit]
  def upsertPaymentMethodForCustomer(
    userId: UUID,
    customerId: String,
    paymentMethodId: String
  ): Future[CustomerPaymentMethod]
  def getPaymentMethodsForCustomer(userId: UUID): Future[Seq[CustomerPaymentMethod]]
  def deletePaymentMethodForCustomer(userId: UUID, paymentMethodId: String): Future[Unit]
}
