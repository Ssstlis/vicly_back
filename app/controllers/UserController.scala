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

  /**
    * @api {POST} /api/user/login  Login
    * @apiName Login in system
    * @apiGroup User
    * @apiParam {String}           login Login of user.
    * @apiParam {String}           password Password of user.
    * @apiParamExample {json} Request-body:
    *                  {
    *                  "login":"ivan_123",
    *                  "password":"ivan1734qwerty"
    *                  }
    * @apiDescription Login user into system. Return JSON message with user info an access token or error.
    */
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

  /**
    * @api {GET} /api/user/logout  Logout
    * @apiName Logout
    * @apiGroup User
    * @apiDescription Notify system. when user want forget token.
    */
  def logout = authUtils.authenticateAction { request =>
    userService.logout(request.user)
    Ok
  }

  /**
    * @api {GET} /api/user/list  Init info
    * @apiName Init info
    * @apiGroup User
    * @apiDescription Return all info about groups, users and group chats for init UI. Also return last message in chats.
    */
  def list = authUtils.authenticateAction { request =>
    val user = request.user
    val (withoutGroup, withGroup) = userService.listAll(user)
    Ok(writesUsersPerGroups(withoutGroup, withGroup))
  }

  /**
    * @api {POST} /api/user/update_password  Update password
    * @apiName Update password
    * @apiGroup User
    * @apiParam {String}           password New password of user.
    * @apiParamExample {json} Request-body:
    *                  {
    *                  "password":"ivan_123"
    *                  }
    * @apiDescription Setup new password and return new user info(like in login request) with new access token.
    */
  def updatePassword = authUtils.authenticateAction(parse.json((__ \ "password").read[String])) { request =>
    val user = request.user
    val password = request.body

    userService.updatePassword(user, password).map { token =>
      Ok(Json.toJson(user)(User.writesWithToken(token)))
    }.getOrElse(BadRequest(Json.obj("error" -> "Error while update password")))
  }

  /**
    * @api {POST} /api/user/archive  Archive
    * @apiGroup User
    */
  def archive(userIdO: Option[Int]) = authUtils.authenticateAction { request =>
    val user = request.user
    val userId = userIdO.getOrElse(user.id)

    if (userId == user.id) userService.findByIdNonArchive(userId).collect {
      case user if userService.archive(userId)(user.groupId).isUpdateOfExisting => Ok
    }.getOrElse(BadRequest) else (for {
      groupId <- user.groupId
      _ <- groupService.findByIdAndOwner(groupId, user._id)
      user <- userService.findByIdNonArchive(userId)
      if user.groupId.contains(groupId) && userService.archive(userId)(user.groupId).isUpdateOfExisting
    } yield {
      Ok
    }).getOrElse(BadRequest)
  }

  /**
    * @api {GET} /api/user/set_status/:status   Set status
    * @apiName Set status
    * @apiGroup User
    * @apiParam {String}           status New status of user(status represented in UI).
    * @apiDescription Setup new status of user(status represented in UI).
    */
  def setStatus(status: String) = authUtils.authenticateAction { request =>
    val user = request.user
    user.groupId.collect {
      case groupId if userService.setStatus(user.id, status)(groupId).isUpdateOfExisting => Ok
    }.getOrElse(BadRequest)
  }

  /**
    * @api {GET} /api/user/clear_status  Remove status
    * @apiName Remove status
    * @apiGroup User
    * @apiDescription Clear status of user.
    */
  def removeStatus = authUtils.authenticateAction { request =>
    val user = request.user
    user.groupId.collect {
      case groupId if userService.removeStatus(user.id)(groupId).isUpdateOfExisting => Ok
    }.getOrElse(BadRequest)
  }
}