package io.fitcentive.public_gateway.domain.config

import com.typesafe.config.Config
import io.fitcentive.sdk.config.PubSubSubscriptionConfig

case class SubscriptionsConfig(userEnablePremiumSubscription: String, userDisablePremiumSubscription: String)
  extends PubSubSubscriptionConfig {
  val subscriptions: Seq[String] =
    Seq(userEnablePremiumSubscription, userDisablePremiumSubscription)
}

object SubscriptionsConfig {
  def fromConfig(config: Config): SubscriptionsConfig =
    SubscriptionsConfig(config.getString("user-enable-premium"), config.getString("user-disable-premium"))
}
