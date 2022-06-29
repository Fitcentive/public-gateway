package io.fitcentive.public_gateway.services

import com.google.inject.ImplementedBy
import io.fitcentive.public_gateway.infrastructure.rest.RestImageService
import io.fitcentive.sdk.error.DomainError
import play.api.libs.ws.WSResponse

import java.io.File
import scala.concurrent.Future

@ImplementedBy(classOf[RestImageService])
trait ImageService {
  def uploadImage(uploadPath: String, uploadFile: File): Future[Either[DomainError, Unit]]
  def fetchImage(imagePath: String, transformParameters: String): Future[WSResponse]
}
