package io.fitcentive.public_gateway.controllers

import io.fitcentive.public_gateway.api.PublicApi
import io.fitcentive.public_gateway.infrastructure.utils.ServerErrorHandler
import io.fitcentive.sdk.play.UserAuthAction
import io.fitcentive.sdk.utils.PlayControllerOps
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PublicController @Inject() (publicApi: PublicApi, userAuthAction: UserAuthAction, cc: ControllerComponents)(
  implicit exec: ExecutionContext
) extends AbstractController(cc)
  with PlayControllerOps
  with ServerErrorHandler {

  def uploadImage(uploadPath: String): Action[AnyContent] =
    userAuthAction.async { implicit userRequest =>
      userRequest.request.body.asMultipartFormData
        .map { multiPartFormData =>
          multiPartFormData
            .file("file")
            .map { uploadFile =>
              publicApi
                .uploadImage(uploadPath, uploadFile.ref.toFile)
                .map(handleEitherResult(_)(_ => Ok))
                .recover(resultErrorAsyncHandler)
            }
            .getOrElse(Future.successful(BadRequest("Missing file in request body")))
        }
        .getOrElse(Future.successful(BadRequest("Missing multipart form data in request body")))

    }

  def fetchImage(imagePath: String, transform: Option[String]): Action[AnyContent] =
    Action.async { _ =>
      val imageExtensionOpt = imagePath.split("\\.").lastOption
      imageExtensionOpt match {
        case Some(extension) =>
          publicApi
            .proxyFetchImage(imagePath, transform.fold("100x100")(identity))
            .map { response =>
              response.status match {
                case OK => Ok(response.bodyAsBytes).as(s"image/$extension")
                case _  => NotFound
              }
            }
        case None => Future.successful(BadRequest("Bad resource requested"))
      }
    }

  def enablePremiumForUser: Action[AnyContent] =
    userAuthAction.async { implicit request =>
      publicApi
        .enablePremiumForUser(request.authorizedUser.userId)
        .map(_ => Accepted)
        .recover(resultErrorAsyncHandler)
    }

}
