package io.fitcentive.public_gateway.services

import com.google.inject.ImplementedBy
import com.stripe.model.{Customer, PaymentMethod, Subscription}
import io.fitcentive.public_gateway.domain.user.User
import io.fitcentive.public_gateway.infrastructure.rest.RestStripeService

import scala.concurrent.Future

@ImplementedBy(classOf[RestStripeService])
trait PaymentService {
  def createCustomer(user: User): Future[Customer]
  def attachPaymentMethodToCustomer(paymentMethodId: String, customerId: String): Future[Unit]
  def removePaymentMethodFromCustomer(paymentMethodId: String, customerId: String): Future[Unit]
  def setPaymentMethodAsDefaultForCustomer(paymentMethodId: String, customerId: String): Future[Unit]
  def setPaymentMethodAsDefaultForSubscription(paymentMethodId: String, subscriptionId: String): Future[Unit]
  def getPaymentMethodForSubscription(subscriptionId: String): Future[String]
  def createSubscription(customerId: String, defaultPaymentMethodId: String): Future[Subscription]
  def cancelSubscription(subscriptionId: String): Future[Unit]
  def getSubscription(subscriptionId: String): Future[Subscription]
  def getProtectedCardInfo(paymentMethodId: String): Future[PaymentMethod]
}
