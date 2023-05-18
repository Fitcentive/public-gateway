package io.fitcentive.public_gateway.domain.config

import com.typesafe.config.Config

case class StripeConfig(baseUrl: String, apiKey: String, webhookSecret: String, productConfig: StripeProductConfig)

object StripeConfig {
  def fromConfig(config: Config): StripeConfig =
    StripeConfig(
      baseUrl = config.getString("base-url"),
      apiKey = config.getString("api-key"),
      webhookSecret = config.getString("webhook-secret"),
      productConfig = StripeProductConfig.fromConfig(config.getConfig("product"))
    )
}
