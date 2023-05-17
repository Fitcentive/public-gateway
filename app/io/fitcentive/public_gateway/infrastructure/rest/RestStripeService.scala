package io.fitcentive.public_gateway.infrastructure.rest

import com.stripe.Stripe
import com.stripe.model.{Customer, PaymentMethod, Subscription}
import com.stripe.param.{
  CustomerCreateParams,
  CustomerUpdateParams,
  PaymentMethodAttachParams,
  PaymentMethodDetachParams,
  SubscriptionCancelParams,
  SubscriptionCreateParams,
  SubscriptionUpdateParams
}
import io.fitcentive.public_gateway.domain.config.StripeConfig
import io.fitcentive.public_gateway.domain.user.User
import io.fitcentive.public_gateway.services.{PaymentService, SettingsService}
import play.api.libs.ws.WSClient

import scala.jdk.CollectionConverters._
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class RestStripeService @Inject() (wsClient: WSClient, settingsService: SettingsService)(implicit ec: ExecutionContext)
  extends PaymentService {

  val stripeConfig: StripeConfig = settingsService.stripeConfig
  val stripeBaseUrl: String = stripeConfig.baseUrl

  Stripe.apiKey = stripeConfig.apiKey

  // todo - Turn off premium every 32 days? Whenever a payment renewal goes through, re-enable?

  /**
    * 1. Use idempotency key with stripe APIs
    * 2. Store userId in customer metadata information - we STILL need a backend mapping
    * 3.
    * @return
    */
  override def createCustomer(user: User): Future[Customer] = {
    val params = CustomerCreateParams.builder
      .setDescription(s"Stripe customer for username: ${user.username}")
      .setEmail(user.email)
      .setMetadata(Map("userId" -> user.id.toString).asJava)
      .build

    Future.fromTry {
      Try {
        Customer.create(params)
      }
    }
  }

  override def removePaymentMethodFromCustomer(paymentMethodId: String, customerId: String): Future[Unit] = {
    val detachParams = PaymentMethodDetachParams
      .builder()
      .build()

    Future
      .fromTry {
        Try {
          val paymentMethod = PaymentMethod.retrieve(paymentMethodId)
          paymentMethod.detach(detachParams)
        }
      }
  }

  override def attachPaymentMethodToCustomer(paymentMethodId: String, customerId: String): Future[Unit] = {
    val attachParams = PaymentMethodAttachParams
      .builder()
      .setCustomer(customerId)
      .build()

    Future
      .fromTry {
        Try {
          val paymentMethod = PaymentMethod.retrieve(paymentMethodId)
          paymentMethod.attach(attachParams)
        }
      }
  }

  override def getPaymentMethodForSubscription(subscriptionId: String): Future[String] = {
    Future.fromTry {
      Try {
        val sub = Subscription.retrieve(subscriptionId)
        sub.getDefaultPaymentMethod
      }
    }
  }

  override def setPaymentMethodAsDefaultForSubscription(
    paymentMethodId: String,
    subscriptionId: String
  ): Future[Unit] = {
    val subscriptionUpdateParams = SubscriptionUpdateParams
      .builder()
      .setDefaultPaymentMethod(paymentMethodId)
      .build()

    Future.fromTry {
      Try {
        val sub = Subscription.retrieve(subscriptionId)
        sub.update(subscriptionUpdateParams)
      }
    }
  }

  override def setPaymentMethodAsDefaultForCustomer(paymentMethodId: String, customerId: String): Future[Unit] = {
    val updateParams = CustomerUpdateParams
      .builder()
      .setInvoiceSettings(
        CustomerUpdateParams.InvoiceSettings.builder().setDefaultPaymentMethod(paymentMethodId).build()
      )
      .build()

    Future
      .fromTry {
        Try {
          val customer = Customer.retrieve(customerId)
          customer.update(updateParams)
        }
      }
  }

  override def createSubscription(customerId: String, defaultPaymentMethodId: String): Future[Subscription] = {
    val params = SubscriptionCreateParams
      .builder()
      .addItem(
        SubscriptionCreateParams.Item
          .builder()
          .setPrice(stripeConfig.productConfig.priceId)
          .build()
      )
      .setCustomer(customerId)
      .setCurrency("CAD")
      .setDefaultPaymentMethod(defaultPaymentMethodId)
      .setDescription(s"Fitcentive+ subscription for customerId: $customerId")
      .build()

    Future.fromTry {
      Try {
        Subscription.create(params)
      }
    }
  }

  override def cancelSubscription(subscriptionId: String): Future[Unit] = {
    for {
      subscription <- Future.fromTry {
        Try {
          Subscription.retrieve(subscriptionId)
        }
      }
      _ <- Future.fromTry {
        Try {
          val params = SubscriptionCancelParams.builder().build()
          subscription.cancel(params)
        }
      }
    } yield ()
  }

  override def getSubscription(subscriptionId: String): Future[Subscription] = {
    Future.fromTry {
      Try {
        Subscription.retrieve(subscriptionId)
      }
    }
  }

  override def getProtectedCardInfo(paymentMethodId: String): Future[PaymentMethod] = {
    Future.fromTry {
      Try {
        PaymentMethod.retrieve(paymentMethodId)
      }
    }
  }
}
