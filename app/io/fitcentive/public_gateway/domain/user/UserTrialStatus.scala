package io.fitcentive.public_gateway.domain.user

import play.api.libs.json.{Json, Reads, Writes}

import java.time.Instant
import java.util.UUID

case class UserTrialStatus(userId: UUID, hasBeenUsed: Boolean, createdAt: Instant, updatedAt: Instant)

object UserTrialStatus {
  implicit lazy val reads: Reads[UserTrialStatus] = Json.reads[UserTrialStatus]
  implicit lazy val writes: Writes[UserTrialStatus] = Json.writes[UserTrialStatus]
}
