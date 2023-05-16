package io.fitcentive.public_gateway.domain.config

import com.typesafe.config.Config

case class StripeProductConfig(productId: String, priceId: String)

object StripeProductConfig {
  def fromConfig(config: Config): StripeProductConfig =
    StripeProductConfig(productId = config.getString("id"), priceId = config.getString("price-id"))
}
