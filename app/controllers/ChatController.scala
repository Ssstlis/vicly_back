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

  /**
    * @api {POST} /api/chat/create  Create new chat
    * @apiName Create new chat
    * @apiGroup Chats
    * @apiParam {Array[Int]}    user_ids Chat members user ids.
    * @apiParam {String}        name     Title of chat.
    * @apiParam {String}           purpose  Purpose for chat.
    * @apiParamExample {json} Request-body:
    * {
    *     "user_ids":[54,12,6],
    *     "name":"New LOL chat",
    *     "purpose":"Fake purpose!"
    * }
    * @apiDescription Create new chat in same group with user, which make request.
    */
  def create = authUtils.authenticateAction(parse.json) { request =>
    val user = request.user
    val json = request.body

    (for {
      groupId <- user.groupId
      userIds <- (json \ "user_ids").asOpt[List[Int]] if userIds.nonEmpty
      name <- (json \ "name").asOpt[String]
      purpose = (json \ "purpose").asOpt[String]
      ids = userIds.distinct
      if chatService.createGroupChat(ids, Some(groupId), user._id, name, purpose)
    } yield {
      socketNotificationService.newGroupChat(groupId, userIds)
      Ok
    }).getOrElse(BadRequest)
  }

  /**
    * @api {POST} /api/chat/add  Add new user in chat
    * @apiName Add new user in chat
    * @apiGroup Chats
    * @apiParam {Array[Int]}    user_ids Chat members user ids.
    * @apiParam {String}        name     Title of chat.
    * @apiParam {String}           purpose  Purpose for chat.
    * @apiParamExample {json} Request-body:
    * {
    *     "user_ids":[54,12,6],
    *     "name":"New LOL chat",
    *     "purpose":"Fake purpose!"
    * }
    * @apiDescription Create new chat in same group with user, which make request.
    */
  def add = authUtils.authenticateAction(parse.json) { request =>
    val json = request.body
    val user = request.user

    (for {
      chatId <- (json \ "chat_id").asOpt[Int]
      userId <- (json \ "user_id").asOpt[Int]
      chat <- chatService.findGroupChat(chatId) if !chat.userIds.contains(userId) && chat.chatType == "group"
      userAdd <- userService.findOne(userId) //if userAdd.groupId.contains(groupId)
      userIds = userId :: chat.userIds if chatService.updateUsers(chatId, userIds).isUpdateOfExisting
    } yield {
      socketNotificationService.newChatForUser(chat.copy(userIds = userIds), userId)
      socketNotificationService.newUserInChat(chat, userId)
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
      chat <- chatService.findGroupChat(chatId) if chat.userIds.contains(userId)
      userIds = chat.userIds.filterNot(_ == userId)
      if chatService.updateUsers(chatId, userIds).isUpdateOfExisting
    } yield {
      socketNotificationService.removeUserInChat(groupId, chat, userId)
      Ok
    }).getOrElse(BadRequest)
  }

  def archive = authUtils.authenticateAction(parse.json) { request =>
    val json = request.body
    val user = request.user

    (for {
      groupId <- user.groupId
      chatId <- (json \ "chat_id").asOpt[Int]
      chat <- chatService.findGroupChat(chatId) if chat.owner.contains(user._id)
      if chatService.archive(chatId).isUpdateOfExisting
    } yield {
      socketNotificationService.archiveChat(groupId, chat)
      Ok
    }).getOrElse(BadRequest)
  }

  def typing = authUtils.authenticateAction(parse.json) { request =>
    val json = request.body
    val user = request.user

    (for {
      groupId <- user.groupId
      chatId <- (json \ "chat_id").asOpt[Int]
      //      chatType@("user" | "group") <- (json \ "chat_type").asOpt[String]
      chat <- chatService.findById(chatId)
    } yield {
      socketNotificationService.typing(groupId, user.id, chat)
      Ok
    }).getOrElse(BadRequest)
  }
}
