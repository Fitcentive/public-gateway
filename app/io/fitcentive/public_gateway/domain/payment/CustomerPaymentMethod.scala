package io.fitcentive.public_gateway.domain.payment

import play.api.libs.json.{Json, OFormat}

import java.time.Instant
import java.util.UUID

case class CustomerPaymentMethod(
  userId: UUID,
  customerId: String,
  paymentMethodId: String,
  createdAt: Instant,
  updatedAt: Instant
)

object CustomerPaymentMethod {
  implicit lazy val format: OFormat[CustomerPaymentMethod] = Json.format[CustomerPaymentMethod]
}
