package controllers

import actions.AuthUtils
import com.google.inject.{Inject, Singleton}
import pdi.jwt.JwtJson
import play.api.libs.json.Json
import play.api.mvc.InjectedController
import services.{ConfigService, UserService}
import utils.JsonHelper.ObjectIdFormat

@Singleton
class UserController @Inject()(
  authUtils: AuthUtils,
  config: ConfigService,
  userService: UserService
) extends InjectedController {

  def login = Action(parse.json) { request =>
    val json = request.body
    (for {
      login <- (json \ "login").asOpt[String]
      password <- (json \"password").asOpt[String]
      user <- userService.findByLoginAndPassword(login, password)
    } yield {
      userService.setActive(user)
      val claim = Json.obj("user_id" -> user._id)
      Ok(JwtJson.encode(claim, config.secret_key, config.algo))
    }).getOrElse(BadRequest)
  }

  def logout = authUtils.authenticateAction { request =>
    val user = request.user
    userService.setInactive(user)
    Ok.withHeaders()
  }
}
