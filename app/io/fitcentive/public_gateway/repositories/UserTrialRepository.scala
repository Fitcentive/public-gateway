package io.fitcentive.public_gateway.repositories

import com.google.inject.ImplementedBy
import io.fitcentive.public_gateway.domain.user.UserTrialStatus
import io.fitcentive.public_gateway.infrastructure.database.sql.AnormUserTrialRepository

import java.util.UUID
import scala.concurrent.Future

@ImplementedBy(classOf[AnormUserTrialRepository])
trait UserTrialRepository {
  def hasUserAlreadyEnabledTrial(userId: UUID): Future[Boolean]
  def upsertUserTrialStatus(userId: UUID, hasBeenUsed: Boolean): Future[UserTrialStatus]
  def deleteUserTrialStatus(userId: UUID): Future[Unit]
}
