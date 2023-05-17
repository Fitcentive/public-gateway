package io.fitcentive.public_gateway.domain.payment

import play.api.libs.json.{Json, OFormat}

import java.time.Instant
import java.util.UUID

case class CustomerPaymentMethod(
  id: UUID,
  userId: UUID,
  customerId: String,
  paymentMethodId: String,
  isDefault: Boolean,
  createdAt: Instant,
  updatedAt: Instant
)

object CustomerPaymentMethod {
  implicit lazy val format: OFormat[CustomerPaymentMethod] = Json.format[CustomerPaymentMethod]
}
