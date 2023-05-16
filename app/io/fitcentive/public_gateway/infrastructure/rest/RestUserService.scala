package io.fitcentive.public_gateway.infrastructure.rest

import io.fitcentive.public_gateway.domain.errors.UserServiceError
import io.fitcentive.public_gateway.domain.user.User
import io.fitcentive.public_gateway.infrastructure.utils.ServiceSecretSupport
import io.fitcentive.public_gateway.services.{SettingsService, UserService}
import io.fitcentive.sdk.config.ServerConfig
import io.fitcentive.sdk.error.DomainError
import play.api.http.Status
import play.api.libs.ws.WSClient

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RestUserService @Inject() (wsClient: WSClient, settingsService: SettingsService)(implicit ec: ExecutionContext)
  extends UserService
  with ServiceSecretSupport {

  val userServiceConfig: ServerConfig = settingsService.userServiceConfig
  val baseUrl: String = userServiceConfig.serverUrl

  override def getUser(userId: UUID): Future[Either[DomainError, User]] =
    wsClient
      .url(s"$baseUrl/api/internal/user/$userId")
      .addHttpHeaders("Content-Type" -> "application/json")
      .addServiceSecret(settingsService)
      .get()
      .map { response =>
        response.status match {
          case Status.OK => Right(response.json.as[User])
          case status    => Left(UserServiceError(s"Unexpected status from user-service: $status"))
        }
      }
}
