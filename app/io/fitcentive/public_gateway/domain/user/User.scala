package io.fitcentive.public_gateway.domain.user

import play.api.libs.json.{Json, Reads, Writes}

import java.time.Instant
import java.util.UUID

case class User(
  id: UUID,
  email: String,
  username: Option[String],
  accountStatus: String,
  authProvider: String,
  enabled: Boolean,
  isPremiumEnabled: Boolean,
  createdAt: Instant,
  updatedAt: Instant
)

object User {
  implicit lazy val reads: Reads[User] = Json.reads[User]
  implicit lazy val writes: Writes[User] = Json.writes[User]
}
