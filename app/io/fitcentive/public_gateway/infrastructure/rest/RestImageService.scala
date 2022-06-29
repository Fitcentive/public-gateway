package io.fitcentive.public_gateway.infrastructure.rest

import akka.stream.scaladsl.{FileIO, Source}
import io.fitcentive.public_gateway.domain.config.ProtectedServerConfig
import io.fitcentive.public_gateway.domain.errors.ImageUploadError
import io.fitcentive.public_gateway.services.{ImageService, SettingsService}
import io.fitcentive.sdk.error.DomainError
import play.api.http.Status
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc.MultipartFormData.FilePart

import java.io.File
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RestImageService @Inject() (wsClient: WSClient, settingsService: SettingsService)(implicit ec: ExecutionContext)
  extends ImageService {

  val imageServiceConfig: ProtectedServerConfig = settingsService.imageServiceConfig
  val imageServiceBaseUrl: String = imageServiceConfig.serverUrl

  val imageProxyConfig: ProtectedServerConfig = settingsService.imageProxyConfig
  val imageProxyBaseUrl: String = imageProxyConfig.serverUrl

  override def uploadImage(uploadPath: String, uploadFile: File): Future[Either[DomainError, Unit]] = {
    wsClient
      .url(s"$imageServiceBaseUrl/files/$uploadPath?token=${imageServiceConfig.token}")
      .put(Source(Seq(FilePart("file", uploadPath, Option.empty, FileIO.fromPath(uploadFile.toPath)))))
      .map { response =>
        response.status match {
          case Status.OK => Right()
          case status    => Left(ImageUploadError(s"Unexpected status from image-service: $status"))
        }
      }
  }

  override def fetchImage(imagePath: String, transformParameters: String): Future[WSResponse] =
    wsClient
      .url(s"$imageProxyBaseUrl/$transformParameters/$imagePath?token=${imageServiceConfig.token}")
      .get()
}
