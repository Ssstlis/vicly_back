package controllers

import actions.AuthUtils
import com.google.inject.{Inject, Singleton}
import models.User
import models.json.UserJson._
import pdi.jwt.JwtJson
import play.api.libs.json.Json
import play.api.mvc.InjectedController
import services.{ConfigService, GroupService, UserService}
import utils.CollectionHelper.TraversableOnceHelper
import utils.JsonHelper.ObjectIdFormat

@Singleton
class UserController @Inject()(
  authUtils: AuthUtils,
  config: ConfigService,
  groupService: GroupService,
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
      val token = JwtJson.encode(claim, config.secret_key, config.algo)
      Ok(Json.toJson(user)(User.writesWithToken(token)))
    }).getOrElse(BadRequest)
  }

  def logout = authUtils.authenticateAction { request =>
    val user = request.user
    userService.setInactive(user)
    Ok.withHeaders()
  }

  def signup = Action(parse.json[User]) { request =>
    val user = request.body
    userService.findByLogin(user.login).fold {
      userService.create(user)
      Ok
    }(_ => BadRequest)
  }

  def list = authUtils.authenticateAction { _ =>
    val groups = groupService.all.zipBy(_.id)

    val (usersWithoutGroup, usersWithGroups) = userService.all
      .groupBy(_.groupId)
      .map { case (groupId, users) =>
        groups.get(groupId).map(group => Right(group -> users)).getOrElse(Left(users))
      }.toList.partition(_.isLeft)

    val withoutGroup = usersWithoutGroup.collect { case Left(users) => users }.flatten
    val withGroups = usersWithGroups.collect { case Right(value) => value }.toMap

    Ok(writesUsersPerGroups(withoutGroup, withGroups))
  }
}
