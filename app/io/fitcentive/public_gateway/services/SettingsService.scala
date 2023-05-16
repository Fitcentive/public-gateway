package io.fitcentive.public_gateway.services

import com.google.inject.ImplementedBy
import io.fitcentive.public_gateway.domain.config.{
  AdUnitIdsConfig,
  AppPubSubConfig,
  ProtectedServerConfig,
  StripeConfig
}
import io.fitcentive.public_gateway.infrastructure.settings.AppConfigService
import io.fitcentive.sdk.config.{GcpConfig, JwtConfig, SecretConfig, ServerConfig}

@ImplementedBy(classOf[AppConfigService])
trait SettingsService {
  def adConfig: AdUnitIdsConfig
  def serviceAccountStringCredentials: String
  def gcpConfig: GcpConfig
  def pubSubConfig: AppPubSubConfig
  def secretConfig: SecretConfig
  def imageServiceConfig: ProtectedServerConfig
  def imageProxyConfig: ProtectedServerConfig
  def stripeConfig: StripeConfig
  def jwtConfig: JwtConfig
  def keycloakServerUrl: String
  def userServiceConfig: ServerConfig
}
