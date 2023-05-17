package io.fitcentive.public_gateway.domain.payment

import play.api.libs.json.{Json, OFormat}

case class ProtectedCreditCard(
  lastFour: String,
  expiryMonth: Long,
  expiryYear: Long,
  isDefault: Boolean,
  paymentMethodId: String
)

object ProtectedCreditCard {
  implicit lazy val format: OFormat[ProtectedCreditCard] = Json.format[ProtectedCreditCard]
}
