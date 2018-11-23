package controllers

import actions.AuthUtils
import com.google.inject.{Inject, Singleton}
import models.User
import models.json.UserJson._
import pdi.jwt.JwtJson
import play.api.libs.json.{Json, __}
import play.api.mvc.InjectedController
import services._
import utils.CollectionHelper.TraversableOnceHelper

@Singleton
class UserController @Inject()(
  authUtils: AuthUtils,
  chatService: ChatService,
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

      val token = JwtJson.encode(user.claim, config.secret_key, config.algo)
      Ok(Json.toJson(user)(User.writesWithToken(token)))
    }).getOrElse(BadRequest)
  }

  def logout = authUtils.authenticateAction { request =>
    val user = request.user
    userService.setInactive(user)
    Ok.withHeaders()
  }

  def list = authUtils.authenticateAction { request =>
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

    val withGroups = usersWithGroup.collect { case Right((groupId, users)) =>
      val usersWithMessages = users.seq.map { user =>
        val (unread, lastO) = chatService.findUserChat(request.user.id, user.id).map { chat =>
          messageService.findUnreadMessagesCount(chat.id, request.user.id) ->
          messageService.findLastMessage(chat.id, request.user.id)
        }.getOrElse(0L, None)
        (user, unread, lastO)
      }
      val groupChatMap = (for {
        groupId <- request.user.groupId
        chats = chatService.findGroupChatByGroupId(groupId)
      } yield {
        chats.map(chat =>
          chat.id -> (messageService.findUnreadMessagesCount(chat.id), messageService.findLastMessage(chat.id))
        ).toMap
      }).getOrElse(Map.empty)
      groupId -> (usersWithMessages, groupChatMap)
    }.toMap.seq

    Ok(writesUsersPerGroups(withoutGroup, withGroups))
  }

  def updatePassword = authUtils.authenticateAction(parse.json((__ \ "password").read[String])) { request =>
    val user = request.user
    val password = request.body

    if (userService.updatePassword(user.id, password).isUpdateOfExisting) {
      userService.findByLoginAndPassword(user.login, password).map { user =>
        userService.setActive(user)
        userService.updateActivity(user.id)

        val token = JwtJson.encode(user.claim, config.secret_key, config.algo)
        Ok(Json.toJson(user)(User.writesWithToken(token)))
      }.getOrElse(BadRequest)
    } else {
      BadRequest
    }
  }
}
