package io.fitcentive.public_gateway.api

import io.fitcentive.public_gateway.domain.ad.AdType
import io.fitcentive.public_gateway.services.{ImageService, SettingsService}
import io.fitcentive.sdk.error.DomainError
import play.api.libs.ws.WSResponse

import java.io.File
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PublicApi @Inject() (imageService: ImageService, settingsService: SettingsService)(implicit
  ec: ExecutionContext
) {

  def uploadImage(imagePath: String, imageFile: File): Future[Either[DomainError, Unit]] =
    imageService.uploadImage(imagePath, imageFile)

  def proxyFetchImage(imagePath: String, transformParameters: String): Future[WSResponse] =
    imageService.fetchImage(imagePath, transformParameters)

  def getAdUnitId(isAndroid: Boolean, adType: AdType): Future[String] =
    Future.successful {
      if (isAndroid) settingsService.adConfig.androidAdUnitId
      else settingsService.adConfig.iosAdUnitId
    }

}
