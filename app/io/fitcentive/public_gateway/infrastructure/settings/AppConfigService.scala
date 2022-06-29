package io.fitcentive.public_gateway.infrastructure.settings

import com.typesafe.config.Config
import io.fitcentive.public_gateway.domain.config.ProtectedServerConfig
import io.fitcentive.public_gateway.services.SettingsService
import io.fitcentive.sdk.config.{JwtConfig, SecretConfig}
import play.api.Configuration

import javax.inject.{Inject, Singleton}

@Singleton
class AppConfigService @Inject() (config: Configuration) extends SettingsService {

  override def imageServiceConfig: ProtectedServerConfig =
    ProtectedServerConfig.fromConfig(config.get[Config]("services.image-service"))

  override def imageProxyConfig: ProtectedServerConfig =
    ProtectedServerConfig.fromConfig(config.get[Config]("services.image-proxy"))

  override def secretConfig: SecretConfig = SecretConfig.fromConfig(config.get[Config]("services"))

  override def keycloakServerUrl: String = config.get[String]("keycloak.server-url")

  override def jwtConfig: JwtConfig = JwtConfig.apply(config.get[Config]("jwt"))

}
