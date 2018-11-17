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

  def signup = Action(parse.json[User]) { request =>
    val user = request.body
    userService.findByLogin(user.login).fold {
      userService.create(user)
      Ok
    }(_ => BadRequest)
  }

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

  def list = authUtils.authenticateAction {
    val groups = groupService.all.zipBy(_.id)

    val users = userService.all.map {
      case user if user.groupId.isDefined => Right(user)
      case user => Left(user)
    }.view

    val usersWithoutGroup = users.collect { case Left(user) => user }.toList

    val usersWithGroup = users
      .collect { case Right(user) => user }
      .groupBy(_.groupId)
      .collect { case (Some(groupId), users) => groups.get(groupId) -> users }
      .collect {
        case (Some(group), users) => Right(group, users)
        case (_, users) => Left(users)
      }.toList

    val withoutGroup = usersWithGroup.collect { case Left(users) => users }.flatten ::: usersWithoutGroup
    val withGroups = usersWithGroup.collect { case Right((groupId, users)) => groupId -> users.toList }.toMap

    Ok(writesUsersPerGroups(withoutGroup, withGroups))
  }
}
