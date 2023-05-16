package io.fitcentive.public_gateway.infrastructure.utils

import io.fitcentive.public_gateway.services.SettingsService
import play.api.libs.ws.WSRequest

trait ServiceSecretSupport {

  implicit class ServiceSecretHeaders(wsRequest: WSRequest) {
    def addServiceSecret(settingsService: SettingsService): WSRequest =
      wsRequest.addHttpHeaders("Service-Secret" -> settingsService.secretConfig.serviceSecret)
  }
}
