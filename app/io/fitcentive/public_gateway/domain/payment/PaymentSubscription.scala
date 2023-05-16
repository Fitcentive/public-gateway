package io.fitcentive.public_gateway.domain.payment

import play.api.libs.json.{Json, OFormat}

import java.time.Instant
import java.util.UUID

case class PaymentSubscription(
  id: UUID,
  userId: UUID,
  subscriptionId: String,
  customerId: String,
  isActive: Boolean,
  validUntil: Instant,
  createdAt: Instant,
  updatedAt: Instant
)

object PaymentSubscription {
  implicit lazy val format: OFormat[PaymentSubscription] = Json.format[PaymentSubscription]
}
