package controllers

import actions.AuthUtils
import com.google.inject.{Inject, Singleton}
import play.api.mvc.InjectedController
import services.{ChatService, SocketNotificationService, UserService}

@Singleton
class ChatController @Inject()(
  authUtils: AuthUtils,
  chatService: ChatService,
  socketNotificationService: SocketNotificationService,
  userService: UserService
) extends InjectedController {

  def create = authUtils.authenticateAction(parse.json) { request =>
    val user = request.user
    val json = request.body

    (for {
      groupId <- user.groupId
      userIds <- (json \ "user_ids").asOpt[List[Int]] if userIds.nonEmpty
      name <- (json \ "name").asOpt[String]
      purpose = (json \ "purpose").asOpt[String]
      if userService.usersSameGroupNonArchive(userIds, groupId)
      ids = userIds.distinct
      if chatService.createGroupChat(ids, groupId, user._id, name, purpose)
    } yield {
      socketNotificationService.newGroupChat(groupId, userIds)
      Ok
    }).getOrElse(BadRequest)
  }

  def add = authUtils.authenticateAction(parse.json) { request =>
    val json = request.body
    val user = request.user

    (for {
      groupId <- user.groupId
      chatId <- (json \ "chat_id").asOpt[Int]
      _@"group" <- (json \ "chat_type").asOpt[String]
      userId <- (json \ "user_id").asOpt[Int]
      userAdd <- userService.findOne(userId) if userAdd.groupId.contains(groupId)
      chat <- chatService.findGroupChat(chatId, groupId) if !chat.userIds.contains(userId)
      userIds = userId :: chat.userIds if chatService.updateUsers(chatId, userIds).isUpdateOfExisting
    } yield {
      socketNotificationService.newUserInChat(groupId, chatId, userId)
      Ok
    }).getOrElse(BadRequest)
  }

  def remove = authUtils.authenticateAction(parse.json) { request =>
    val json = request.body
    val user = request.user

    (for {
      groupId <- user.groupId
      chatId <- (json \ "chat_id").asOpt[Int]
      _@"group" <- (json \ "chat_type").asOpt[String]
      userId <- (json \ "user_id").asOpt[Int]
      userAdd <- userService.findOne(userId) if userAdd.groupId.contains(groupId)
      chat <- chatService.findGroupChat(chatId, groupId) if chat.userIds.contains(userId)
      userIds = chat.userIds.filterNot(_ == userId)
      if chatService.updateUsers(chatId, userIds).isUpdateOfExisting
    } yield {
      socketNotificationService.removeUserInChat(groupId, chatId, userId)
      Ok
    }).getOrElse(BadRequest)
  }

  def archive = authUtils.authenticateAction(parse.json) { request =>
    val json = request.body
    val user = request.user

    (for {
      groupId <- user.groupId
      chatId <- (json \ "chat_id").asOpt[Int]
      chat <- chatService.findGroupChat(chatId, groupId) if chat.owner.contains(user._id)
      if chatService.archive(chatId).isUpdateOfExisting
    } yield {
      socketNotificationService.archiveChat(groupId, chatId)
      Ok
    }).getOrElse(BadRequest)
  }

  def typing = authUtils.authenticateAction(parse.json) { request =>
    val json = request.body
    val user = request.user

    (for {
      groupId <- user.groupId
      chatId <- (json \ "chat_id").asOpt[Int]
      chatType@("user" | "group") <- (json \ "chat_type").asOpt[String]
      _ <- chatType match {
        case "user" => chatService.findUserChat(user.id, chatId)
        case "group" => chatService.findGroupChat(chatId, groupId)
      }
    } yield {
      socketNotificationService.typing(groupId, user.id, chatId, chatType)
      Ok
    }).getOrElse(BadRequest)
  }
}
