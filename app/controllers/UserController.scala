package controllers

import actions.AuthUtils
import com.google.inject.{Inject, Singleton}
import models.User
import models.json.UserJson._
import pdi.jwt.JwtJson
import play.api.libs.json.Json
import play.api.libs.json.__
import play.api.mvc.InjectedController
import services._
import utils.CollectionHelper.TraversableOnceHelper
import utils.JsonHelper.ObjectIdFormat
import utils.Helper.StringExtended

@Singleton
class UserController @Inject()(
  authUtils: AuthUtils,
  config: ConfigService,
  groupService: GroupService,
  inviteService: InviteService,
  messageService: MessageService,
  userService: UserService
) extends InjectedController {

  def signup = Action(parse.json(User.reads(userService.nextId))) { request =>
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
      userService.updateActivity(user.id)
      val claim = Json.obj(
        "user_id" -> user._id,
        "login" -> user.login,
        "password" -> user.password
      )
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
    }.par

    val usersWithoutGroup = users.collect { case Left(user) => user }.toList

    val usersWithGroup = users
      .collect { case Right(user) => user }
      .groupBy(_.groupId)
      .map { case (groupIdO, users) => {
        (for {
          groupId <- groupIdO
          group <- groups.get(groupId)
        } yield {
          Right(group -> users)
        }).getOrElse(Left(users))
      }
    }

    val withoutGroup = usersWithoutGroup ::: usersWithGroup.flatMap {
      case Left(users) => users
      case _ => List.empty[User]
    }.toList

    val withGroups = usersWithGroup.collect { case Right((groupId, users)) => groupId -> users.seq }.toMap.seq

    Ok(writesUsersPerGroups(withoutGroup, withGroups))
  }

  def updatePassword = authUtils.authenticateAction(parse.json((__ \ "password").read[String])) { request =>
    if (userService.updatePassword(request.user.id, request.body).isUpdateOfExisting) {
      Ok
    } else {
      BadRequest
    }
  }
}
