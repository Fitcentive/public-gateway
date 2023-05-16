package io.fitcentive.public_gateway.infrastructure.settings

import com.typesafe.config.Config
import io.fitcentive.public_gateway.domain.config.{
  AdUnitIdsConfig,
  AppPubSubConfig,
  ProtectedServerConfig,
  StripeConfig,
  SubscriptionsConfig,
  TopicsConfig
}
import io.fitcentive.public_gateway.services.SettingsService
import io.fitcentive.sdk.config.{GcpConfig, JwtConfig, SecretConfig, ServerConfig}
import play.api.Configuration

import javax.inject.{Inject, Singleton}

@Singleton
class AppConfigService @Inject() (config: Configuration) extends SettingsService {

  override def userServiceConfig: ServerConfig = ServerConfig.fromConfig(config.get[Config]("services.user-service"))

  override def stripeConfig: StripeConfig = StripeConfig.fromConfig(config.get[Config]("stripe"))

  override def adConfig: AdUnitIdsConfig =
    AdUnitIdsConfig.fromConfig(config.get[Config]("ads.ad-unit-ids"))

  override def serviceAccountStringCredentials: String =
    config.get[String]("gcp.pubsub.service-account-string-credentials")

  override def gcpConfig: GcpConfig =
    GcpConfig(project = config.get[String]("gcp.project"))

  override def pubSubConfig: AppPubSubConfig =
    AppPubSubConfig(
      topicsConfig = TopicsConfig.fromConfig(config.get[Config]("gcp.pubsub.topics")),
      subscriptionsConfig = SubscriptionsConfig.fromConfig(config.get[Config]("gcp.pubsub.subscriptions"))
    )

  override def imageServiceConfig: ProtectedServerConfig =
    ProtectedServerConfig.fromConfig(config.get[Config]("services.image-service"))

  override def imageProxyConfig: ProtectedServerConfig =
    ProtectedServerConfig.fromConfig(config.get[Config]("services.image-proxy"))

  override def secretConfig: SecretConfig = SecretConfig.fromConfig(config.get[Config]("services"))

  override def keycloakServerUrl: String = config.get[String]("keycloak.server-url")

  override def jwtConfig: JwtConfig = JwtConfig.apply(config.get[Config]("jwt"))

}
