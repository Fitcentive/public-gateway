package io.fitcentive.public_gateway.domain.payment

import play.api.libs.json.JsonNaming.SnakeCase
import play.api.libs.json.{Json, JsonConfiguration, OFormat}

import java.time.Instant
import java.util.UUID

case class StripeSubscription(
  id: UUID,
  userId: UUID,
  subscriptionId: String,
  customerId: String,
  startedAt: Instant,
  validUntil: Instant,
  trialEnd: Option[Instant],
  createdAt: Instant,
  updatedAt: Instant
)

object StripeSubscription {
  implicit val config = JsonConfiguration(SnakeCase)
  implicit lazy val format: OFormat[StripeSubscription] = Json.format[StripeSubscription]
}
