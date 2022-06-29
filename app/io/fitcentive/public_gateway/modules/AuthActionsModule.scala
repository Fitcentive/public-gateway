package io.fitcentive.public_gateway.modules

import com.google.inject.{AbstractModule, Provides}
import io.fitcentive.public_gateway.services.SettingsService
import io.fitcentive.sdk.config.JwtConfig
import io.fitcentive.sdk.domain.{PublicKeyRepository, TokenValidationService}
import io.fitcentive.sdk.infrastructure.{
  AuthTokenValidationService,
  CachedKeycloakPublicKeyRepository,
  KeycloakPublicKeyRepository
}

import javax.inject.Singleton

class AuthActionsModule extends AbstractModule {

  /**
    * NOTE: The following 3 providers include sdk dependencies for Auth actions
    * Without them, you will not be able to invoke authentication actions and will run into runtime Guice errors
    */

  @Provides
  @Singleton
  def provideJwtConfig(settingsService: SettingsService): JwtConfig =
    settingsService.jwtConfig

  @Provides
  @Singleton
  def provideCachedKeycloakPublicKeyRepository(settingsService: SettingsService): PublicKeyRepository = {
    val underlying = new KeycloakPublicKeyRepository(settingsService.keycloakServerUrl)
    new CachedKeycloakPublicKeyRepository(underlying)
  }

  @Provides
  @Singleton
  def provideJwtTokenValidationService(
    settingsService: SettingsService,
    publicKeyRepository: PublicKeyRepository
  ): TokenValidationService =
    new AuthTokenValidationService(settingsService.jwtConfig, settingsService.secretConfig, publicKeyRepository)

}
