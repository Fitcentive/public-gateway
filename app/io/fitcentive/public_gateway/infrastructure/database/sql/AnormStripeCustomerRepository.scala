package io.fitcentive.public_gateway.infrastructure.database.sql

import anorm.{Macro, RowParser}
import io.fitcentive.public_gateway.domain.payment.{PaymentCustomer, PaymentSubscription}
import io.fitcentive.public_gateway.repositories.CustomerRepository
import io.fitcentive.sdk.infrastructure.contexts.DatabaseExecutionContext
import io.fitcentive.sdk.infrastructure.database.DatabaseClient
import play.api.db.Database

import java.time.Instant
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import scala.util.chaining.scalaUtilChainingOps

@Singleton
class AnormStripeCustomerRepository @Inject() (val db: Database)(implicit val dbec: DatabaseExecutionContext)
  extends CustomerRepository
  with DatabaseClient {

  import AnormStripeCustomerRepository._

  override def getCustomerByUserId(userId: UUID): Future[Option[PaymentCustomer]] =
    Future {
      getRecordOpt(SQL_GET_CUSTOMER_BY_USER_ID, "userId" -> userId)(customerRowParser).map(_.toDomain)
    }

  override def addCustomer(userId: UUID, customerId: String): Future[PaymentCustomer] =
    Future {
      Instant.now.pipe { now =>
        executeSqlWithExpectedReturn[CustomerRow](
          SQL_ADD_CUSTOMER,
          Seq("userId" -> userId, "customerId" -> customerId, "now" -> now)
        )(customerRowParser).toDomain
      }
    }

  override def getSubscriptionsForUser(userId: UUID): Future[Seq[PaymentSubscription]] =
    Future {
      getRecords(SQL_GET_SUBSCRIPTIONS_FOR_USER, "userId" -> userId)(subscriptionRowParser)
        .map(_.toDomain)
    }

  override def createSubscriptionForUser(
    id: UUID,
    userId: UUID,
    subscriptionId: String,
    customerId: String,
    isActive: Boolean,
    validUntil: Instant
  ): Future[PaymentSubscription] =
    Future {
      Instant.now.pipe { now =>
        executeSqlWithExpectedReturn[SubscriptionRow](
          SQL_CREATE_SUBSCRIPTIONS_FOR_USER,
          Seq(
            "id" -> id,
            "userId" -> userId,
            "subscriptionId" -> subscriptionId,
            "customerId" -> customerId,
            "isActive" -> isActive,
            "validUntil" -> validUntil,
            "now" -> now
          )
        )(subscriptionRowParser).toDomain
      }
    }

}

object AnormStripeCustomerRepository {
  private val SQL_GET_CUSTOMER_BY_USER_ID: String =
    s"""
       |select * 
       |from stripe_users s
       |where s.user_id = {userId}::uuid ;
       |""".stripMargin

  private val SQL_ADD_CUSTOMER: String =
    s"""
       |insert into stripe_users (user_id, customer_id, created_at, updated_at)
       |values ({userId}::uuid, {customerId}, {now}, {now})
       |returning * ;
       |""".stripMargin

  private val SQL_CREATE_SUBSCRIPTIONS_FOR_USER: String =
    s"""
       |insert into stripe_user_subscriptions (id, user_id, subscription_id, customer_id, is_active, valid_until, created_at, updated_at)
       |values ({id}::uuid, {userId}::uuid, {subscriptionId}, {customerId}, {isActive}, {validUntil}, {now}, {now})
       |returning * ;
       |""".stripMargin

  private val SQL_GET_SUBSCRIPTIONS_FOR_USER: String =
    s"""
       |select *
       |from stripe_user_subscriptions 
       |where user_id = {userId}::uuid ;
       |""".stripMargin

  private case class CustomerRow(user_id: UUID, customer_id: String, created_at: Instant, updated_at: Instant) {
    def toDomain: PaymentCustomer =
      PaymentCustomer(userId = user_id, customerId = customer_id, createdAt = created_at, updatedAt = updated_at)
  }

  private case class SubscriptionRow(
    id: UUID,
    user_id: UUID,
    subscription_id: String,
    customer_id: String,
    is_active: Boolean,
    valid_until: Instant,
    created_at: Instant,
    updated_at: Instant
  ) {
    def toDomain: PaymentSubscription =
      PaymentSubscription(
        id = id,
        userId = user_id,
        subscriptionId = subscription_id,
        customerId = customer_id,
        isActive = is_active,
        validUntil = valid_until,
        createdAt = created_at,
        updatedAt = updated_at
      )
  }

  private val customerRowParser: RowParser[CustomerRow] = Macro.namedParser[CustomerRow]
  private val subscriptionRowParser: RowParser[SubscriptionRow] = Macro.namedParser[SubscriptionRow]
}
