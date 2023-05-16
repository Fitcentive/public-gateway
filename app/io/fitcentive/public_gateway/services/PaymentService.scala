package io.fitcentive.public_gateway.services

import com.google.inject.ImplementedBy
import com.stripe.model.{Customer, PaymentMethod, Subscription}
import io.fitcentive.public_gateway.domain.payment.CreditCard
import io.fitcentive.public_gateway.domain.user.User
import io.fitcentive.public_gateway.infrastructure.rest.RestStripeService

import scala.concurrent.Future

@ImplementedBy(classOf[RestStripeService])
trait PaymentService {
  def createCustomer(user: User): Future[Customer]
  def createCardPaymentMethod(creditCard: CreditCard): Future[PaymentMethod]
  def attachPaymentMethodToCustomer(paymentMethodId: String, customerId: String): Future[Unit]
  def createSubscription(customerId: String, defaultPaymentMethodId: String): Future[Subscription]
}
