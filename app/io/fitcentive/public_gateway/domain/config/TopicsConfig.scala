package io.fitcentive.public_gateway.domain.config

import com.typesafe.config.Config
import io.fitcentive.sdk.config.PubSubTopicConfig

case class TopicsConfig(userEnablePremiumTopic: String, userDisablePremiumTopic: String) extends PubSubTopicConfig {

  val topics: Seq[String] =
    Seq(userEnablePremiumTopic, userDisablePremiumTopic)

}

object TopicsConfig {
  def fromConfig(config: Config): TopicsConfig =
    TopicsConfig(config.getString("user-enable-premium"), config.getString("user-disable-premium"))
}
