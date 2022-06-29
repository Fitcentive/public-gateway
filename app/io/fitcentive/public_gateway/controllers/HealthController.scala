package io.fitcentive.public_gateway.controllers

import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import javax.inject.{Inject, Singleton}

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class HealthController @Inject() (cc: ControllerComponents) extends AbstractController(cc) {

  def healthCheck: Action[AnyContent] = Action { Ok("Server is alive!") }

}
