package io.fitcentive.public_gateway.services

import com.google.inject.ImplementedBy
import io.fitcentive.public_gateway.domain.config.ProtectedServerConfig
import io.fitcentive.public_gateway.infrastructure.settings.AppConfigService
import io.fitcentive.sdk.config.{JwtConfig, SecretConfig}

@ImplementedBy(classOf[AppConfigService])
trait SettingsService {
  def secretConfig: SecretConfig
  def imageServiceConfig: ProtectedServerConfig
  def imageProxyConfig: ProtectedServerConfig
  def jwtConfig: JwtConfig
  def keycloakServerUrl: String
}
