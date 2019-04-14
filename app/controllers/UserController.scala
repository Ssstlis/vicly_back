package controllers

import actions.AuthUtils
import com.google.inject.{Inject, Singleton}
import io.swagger.annotations._
import io.swagger.models.Response
import models.User
import models.json.UserJson._
import pdi.jwt.JwtJson
import play.api.libs.json.{Json, __}
import play.api.mvc.InjectedController
import services._
import utils.CollectionHelper.TraversableOnceHelper

@Api(value = "User actions Controller", produces = "application/json")
@Singleton
class UserController @Inject()(
                                authUtils: AuthUtils,
                                chatService: ChatService,
                                config: ConfigService,
                                groupService: GroupService,
                                inviteService: InviteService,
                                messageService: MessageService,
                                socketNotificationService: SocketNotificationService,
                                userService: UserService
                              ) extends InjectedController {

  def signup = Action(parse.json(User.reads(userService.nextId))) { request =>
    val user = request.body
    userService.findByLogin(user.login).fold {
      userService.create(user)
      Ok
    }(_ => BadRequest)
  }

  @ApiOperation(
    value = "Login user",
    notes = "Return user object with token",
    response = classOf[models.User],
  )
  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "Invalid login or password"),
    new ApiResponse(code = 200, message = "Login successful")))
  def login = Action(parse.json) { request =>
    val json = request.body
    (for {login <- (json \ "login").asOpt[String]
      password <- (json \ "password").asOpt[String]
      user <- userService.findByLoginAndPassword(login, password)
    } yield {
      userService.setActive(user)
      userService.updateActivity(user.id)(user.groupId)

      val token = JwtJson.encode(user.claim, config.secret_key, config.algo)
      Ok(Json.toJson(user)(User.writesWithToken(token)))
    }).getOrElse(BadRequest)
  }

  def logout = authUtils.authenticateAction { request =>
    val user = request.user
    socketNotificationService.offline(user.groupId, user.id)
    userService.setInactive(user)
    Ok.withHeaders()
  }

  def list = authUtils.authenticateAction { request =>
    val user = request.user
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

    val withGroups = usersWithGroup.collect {
      case Right((group, users)) =>
        val usersWithMessages = users.seq.map { user =>
          val chat = chatService.findUserChat(request.user.id, user.id)
          val (unread, lastO) = chat.map { chat =>
            messageService.findUnreadMessagesCount(chat.id, request.user.id) ->
              messageService.findLastMessage(chat.id)
          }.getOrElse(0L, None)
          (user, unread, lastO, chat)
        }
        val groupChatMap = chatService.findGroupChatByGroupId(group.id)
          .filter(chat => chat.groupId == user.groupId)
          .map(chat =>
          chat.id -> (messageService.findUnreadMessagesCount(chat.id), messageService.findLastMessage(chat.id), chat)
        ).toMap
        group -> (usersWithMessages, groupChatMap)
    }.toMap.seq

    Ok(writesUsersPerGroups(withoutGroup, withGroups))
  }

  def updatePassword = authUtils.authenticateAction(parse.json((__ \ "password").read[String])) { request =>
    val user = request.user
    val password = request.body

    if (userService.updatePassword(user.id, password).isUpdateOfExisting) {
      userService.findByLoginAndPassword(user.login, password).map { user =>
        userService.setActive(user)
        userService.updateActivity(user.id)(user.groupId)

        val token = JwtJson.encode(user.claim, config.secret_key, config.algo)
        Ok(Json.toJson(user)(User.writesWithToken(token)))
      }.getOrElse(BadRequest)
    } else {
      BadRequest
    }
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