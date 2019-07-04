package controllers

import actions.AuthUtils
import com.google.inject.{Inject, Singleton}
import models.User
import models.json.UserJson._
import play.api.libs.json.{Json, __}
import play.api.mvc.InjectedController
import services._

@Singleton
class UserController @Inject()(
                                authUtils: AuthUtils,
                                groupService: GroupService,
                                inviteService: InviteService,
                                socketNotificationService: SocketNotificationService,
                                userService: UserService
                              ) extends InjectedController {

  def signup = Action(parse.json(User.reads(userService.nextId))) { request =>
    val user = request.body
    userService.signup(user).fold {
      Ok
    }(_ => BadRequest)
  }

  def login = Action(parse.json) { request =>
    val json = request.body
    (for {
      login <- (json \ "login").asOpt[String]
      password <- (json \ "password").asOpt[String]
      (token, user) <- userService.login(login, password)
    } yield {
      Ok(Json.toJson(user)(User.writesWithToken(token)))
    }).getOrElse(BadRequest(Json.obj("error" -> "Login or password incorrect")))
  }

  def logout = authUtils.authenticateAction { request =>
    userService.logout(request.user)
    Ok
  }

  def list = authUtils.authenticateAction { request =>
    val user = request.user
    val (withoutGroup, withGroup) = userService.listAll(user)
    Ok(writesUsersPerGroups(withoutGroup, withGroup))
  }

  def updatePassword = authUtils.authenticateAction(parse.json((__ \ "password").read[String])) { request =>
    val user = request.user
    val password = request.body

    userService.updatePassword(user, password).map { token =>
      Ok(Json.toJson(user)(User.writesWithToken(token)))
    }.getOrElse(BadRequest(Json.obj("error" -> "Error while update password")))
  }

  def archive(userIdO: Option[Int]) = authUtils.authenticateAction { request =>
    val user = request.user
    val userId = userIdO.getOrElse(user.id)

    if (userId == user.id) {
      userService.findByIdNonArchive(userId).collect {
        case user if userService.archive(userId)(user.groupId).isUpdateOfExisting => Ok
      }.getOrElse(BadRequest)
    } else {
      (for {
        groupId <- user.groupId
        _ <- groupService.findByIdAndOwner(groupId, user._id)
        user <- userService.findByIdNonArchive(userId)
        if user.groupId.contains(groupId) && userService.archive(userId)(user.groupId).isUpdateOfExisting
      } yield {
        Ok
      }).getOrElse(BadRequest)
    }
  }

  def setStatus(status: String) = authUtils.authenticateAction { request =>
    val user = request.user
    user.groupId.collect {
      case groupId if userService.setStatus(user.id, status)(groupId).isUpdateOfExisting => Ok
    }.getOrElse(BadRequest)
  }

  def removeStatus = authUtils.authenticateAction { request =>
    val user = request.user
    user.groupId.collect {
      case groupId if userService.removeStatus(user.id)(groupId).isUpdateOfExisting => Ok
    }.getOrElse(BadRequest)
  }

  def removeAvatar = authUtils.authenticateAction { request =>
    val user = request.user
    user.groupId.collect {
      case groupId if userService.removeAvatar(user.id)(groupId).isUpdateOfExisting => Ok
    }.getOrElse(BadRequest)
  }
}