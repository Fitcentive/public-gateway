package io.fitcentive.public_gateway.domain.payment

import play.api.libs.json.{Json, OFormat}

case class CreditCard(cardNumber: String, cvc: String, expiryMonth: Int, expiryYear: Int)

object CreditCard {
  implicit lazy val format: OFormat[CreditCard] = Json.format[CreditCard]
}
