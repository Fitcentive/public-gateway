package io.fitcentive.public_gateway.domain.payment

import play.api.libs.json.{Json, OFormat}

import java.time.Instant
import java.util.UUID

case class PaymentCustomer(userId: UUID, customerId: String, createdAt: Instant, updatedAt: Instant)

object PaymentCustomer {
  implicit lazy val format: OFormat[PaymentCustomer] = Json.format[PaymentCustomer]
}
