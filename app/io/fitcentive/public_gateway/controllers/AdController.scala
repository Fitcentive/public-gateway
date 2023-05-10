package io.fitcentive.public_gateway.controllers

import io.fitcentive.public_gateway.api.PublicApi
import io.fitcentive.public_gateway.domain.ad.AdType
import io.fitcentive.public_gateway.infrastructure.utils.ServerErrorHandler
import io.fitcentive.sdk.play.UserAuthAction
import io.fitcentive.sdk.utils.PlayControllerOps
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AdController @Inject() (publicApi: PublicApi, userAuthAction: UserAuthAction, cc: ControllerComponents)(implicit
  exec: ExecutionContext
) extends AbstractController(cc)
  with PlayControllerOps
  with ServerErrorHandler {

  def getAdUnitId(isAndroid: Boolean, adType: String): Action[AnyContent] =
    userAuthAction.async { implicit userRequest =>
      publicApi
        .getAdUnitId(isAndroid, AdType(adType))
        .map(id => Ok(Json.toJson(id)))
        .recover(resultErrorAsyncHandler)
    }

}
