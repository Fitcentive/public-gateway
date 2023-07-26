package io.fitcentive.public_gateway.infrastructure.database.sql

import anorm.{Macro, RowParser}
import io.fitcentive.public_gateway.domain.payment.PaymentSubscription
import io.fitcentive.public_gateway.domain.user.UserTrialStatus
import io.fitcentive.public_gateway.infrastructure.database.sql.AnormStripeCustomerRepository.UserPaymentMethodRow
import io.fitcentive.public_gateway.repositories.{CustomerRepository, UserTrialRepository}
import io.fitcentive.sdk.infrastructure.contexts.DatabaseExecutionContext
import io.fitcentive.sdk.infrastructure.database.DatabaseClient
import play.api.db.Database

import java.time.Instant
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import scala.util.chaining.scalaUtilChainingOps

@Singleton
class AnormUserTrialRepository @Inject() (val db: Database)(implicit val dbec: DatabaseExecutionContext)
  extends UserTrialRepository
  with DatabaseClient {

  import AnormUserTrialRepository._

  override def hasUserAlreadyEnabledTrial(userId: UUID): Future[Boolean] =
    Future {
      getRecordOpt(SQL_GET_USER_TRIAL_STATUS, "userId" -> userId)(userTrialStatusRowParser).exists(x => x.has_been_used)
    }

  override def upsertUserTrialStatus(userId: UUID, hasBeenUsed: Boolean): Future[UserTrialStatus] =
    Future {
      Instant.now.pipe { now =>
        executeSqlWithExpectedReturn(
          SQL_UPSERT_USER_TRIAL_STATUS,
          Seq("userId" -> userId, "hasBeenUsed" -> hasBeenUsed, "now" -> now)
        )(userTrialStatusRowParser).toDomain
      }
    }

  override def deleteUserTrialStatus(userId: UUID): Future[Unit] =
    Future {
      executeSqlWithoutReturning(SQL_DELETE_USER_TRIAL_STATUS, Seq("userId" -> userId))
    }
}

object AnormUserTrialRepository {

  private val SQL_DELETE_USER_TRIAL_STATUS: String =
    """
      |delete from user_trial_status
      |where user_id = {userId}::uuid ;
      |""".stripMargin

  private val SQL_GET_USER_TRIAL_STATUS: String =
    """
      |select * 
      |from user_trial_status
      |where user_id = {userId}::uuid ;
      |""".stripMargin

  private val SQL_UPSERT_USER_TRIAL_STATUS: String =
    """
      |insert into user_trial_status(user_id, has_been_used, created_at, updated_at)
      |values ({userId}::uuid, {hasBeenUsed}, {now}, {now})
      |on conflict (user_id)
      |do update set
      | has_been_used = {hasBeenUsed},
      | updated_at = {now}
      |returning * ;
      |""".stripMargin

  private case class UserTrialStatusRow(
    user_id: UUID,
    has_been_used: Boolean,
    created_at: Instant,
    updated_at: Instant
  ) {
    def toDomain: UserTrialStatus =
      UserTrialStatus(userId = user_id, hasBeenUsed = has_been_used, createdAt = created_at, updatedAt = updated_at)
  }

  private val userTrialStatusRowParser: RowParser[UserTrialStatusRow] = Macro.namedParser[UserTrialStatusRow]
}
