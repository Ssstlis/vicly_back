package controllers

import actions.AuthUtils
import com.google.inject.{Inject, Singleton}
import models.Message
import org.bson.types.ObjectId
import play.api.libs.json.Json
import play.api.mvc.InjectedController
import services.{ChatService, MessageService, UserService}
import utils.JsonHelper.ObjectIdFormat

@Singleton
class MessageController @Inject()(
                                   authUtils: AuthUtils,
                                   chatService: ChatService,
                                   messageService: MessageService,
                                   userService: UserService
                                 ) extends InjectedController {

  /**
    * @api {POST} /api/message/postnewchat  Post in group chat
    * @apiName  Post in group chat
    * @apiGroup Message
    * @apiParam {Array[String]}    attachments  Array of attachments ids in message.
    * @apiParam {Int}              chat_id      Chat id for post message.
    * @apiParam {String}           message      Message text.
    * @apiParamExample {json} Request-body:
    *                  {
    *                  "attachments": []
    *                  "chat_id": 11
    *                  "message": "PRIVET PRIVET"
    *                  }
    * @apiDescription Send message in group chat.
    */
  def sendMessageInChat = authUtils.authenticateAction(parse.json) { request =>
    implicit val user = request.user
    val json = request.body

    (for {
      message <- json.asOpt(Message.reads(user.id))
    } yield {
      messageService.sendMessageInChat(message).fold {
        BadRequest
      }(_ => Ok)
    }).getOrElse(BadRequest(Json.obj("error" -> "Wrong json!")))
  }

  /**
    * @api {POST} /api/message/postnewuser  Post in user chat
    * @apiName Post in user chat
    * @apiGroup Message
    * @apiParam {Array[String]}    attachments  Array of attachments ids in message.
    * @apiParam {Int}              chat_id      User id for post message.
    * @apiParam {String}           message      Message text.
    * @apiParamExample {json} Request-body:
    *                  {
    *                  "attachments": []
    *                  "chat_id": 11
    *                  "message": "PRIVET PRIVET"
    *                  }
    * @apiDescription Send message in user chat.
    */
  def postnewuser = authUtils.authenticateAction(parse.json) { request =>
    val user = request.user
    val json = request.body

    (for {
      groupId <- user.groupId
      message <- json.asOpt(Message.reads(user.id))
    } yield {
      val replyForO = (json \ "reply_for").asOpt[ObjectId]
      val targetUserId = message.chatId
      if (targetUserId != 0 && userService.findByIdNonArchive(targetUserId).isDefined) chatService.findUserChat(user.id, targetUserId).map { chat =>
        if (
          userService.findOne(targetUserId).isDefined && {
            val filledMessage = message.copy(chatId = chat.id, replyForO = replyForO)
            messageService.save(filledMessage)(chat).wasAcknowledged()
          }
        ) Ok else ResetContent
      }.getOrElse {
        if (
          userService.findOne(targetUserId).isDefined &&
            chatService.createUserChat(user.id, targetUserId, groupId)
        ) chatService.findUserChat(user.id, targetUserId).collect { case chat
            if {
              val filledMessage = message.copy(chatId = chat.id, replyForO = replyForO)
              messageService.save(filledMessage)(chat).wasAcknowledged()
            } => Ok
          }.getOrElse(BadRequest) else BadRequest
      } else BadRequest
    }).getOrElse(BadRequest)
  }

  /**
    * @api {GET} /api/message/chat/group/:chat_id/:page  Get group chat message
    * @apiName Get group chat message
    * @apiGroup Message
    * @apiParam {Int}              chat_id      Chat id.
    * @apiParam {Int}              page         Page of messages paginated list.
    * @apiDescription Return page of messages from chat where every page consist of 20 messages.
    */
  def getGroupChatMessages(chatId: Int, page: Int) = authUtils.authenticateAction { _ =>
    Ok(Json.toJson(messageService.getGroupChatMessages(chatId: Int, page: Int)))
  }

  /**
    * @api {GET} /api/message/chat/user/:user_id/:page       Get user chat message
    * @apiName Get user chat message
    * @apiGroup Message
    * @apiParam {Int}              user_id      User id.
    * @apiParam {Int}              page         Page of messages paginated list.
    * @apiDescription Return page of messages from uesr chat where every page consist of 20 messages.
    */
  def getUserChatMessages(userId: Int, page: Int) = authUtils.authenticateAction { request =>
    implicit val user = request.user
    Ok(Json.toJson(messageService.getUserChatMessages(userId, page)))
  }

  /**
    * @api {GET} /api/message/user/from/:user_id/:message_id  Get user chat message from
    * @apiName Get user chat message from
    * @apiGroup Message
    * @apiParam {Int}              user_id         User id.
    * @apiParam {Int}              message_id      Id of message. which will start point for get last message .
    * @apiDescription Return page of messages from chat.
    */
  def getUserChatMessagesFrom(userId: Int, messageId: String) = authUtils.authenticateAction { request =>
    implicit val user = request.user
    messageService.getUserChatMessagesFrom(userId, messageId) match {
      case Right(messages) => Ok(Json.toJson(messages))
      case Left(error) => BadRequest(Json.obj("error" -> error))
    }
  }

  /**
    * @api {GET} /api/message/group/from/:user_id/:message_id  Get group chat message from
    * @apiName Get group chat message from
    * @apiGroup Message
    * @apiParam {Int}              user_id         User id.
    * @apiParam {Int}              message_id      Id of message. which will start point for get last message .
    * @apiDescription Return page of messages from chat.
    */
  def groupChatMessagesFrom(chatId: Int, messageId: String) = authUtils.authenticateAction { request =>
    chatService.findGroupChat(chatId).map { chat =>
      Ok(Json.toJson(messageService.findMessagesAfter(chat.id, new ObjectId(messageId))))
    }.getOrElse(BadRequest(Json.obj("error" -> s"Can't find group chat with id $chatId")))
  }

  /**
    * @api {POST} /api/message/readnew  Mark readed
    * @apiName Mark readed
    * @apiGroup Message
    * @apiParam {String}              message_id      Id of message which was readed.
    * @apiParamExample {json} Request-body:
    *                  {
    *                  "message_id": "dfdg9040mdfuhv8vdf7y6fd"
    *                  }
    * @apiDescription Notify server and users about readed message. Only non-owners cat read message.
    */
  def readnew = authUtils.authenticateAction(parse.json) { request =>
    val json = request.body
    val user = request.user

    (for {
      id <- (json \ "message_id").asOpt[String] if ObjectId.isValid(id)
      oid = new ObjectId(id)
      chat <- messageService.findChatIdByObjectId(oid).flatMap { chatId =>
        chatService.findById(chatId)
      }
      message <- messageService.findById(new ObjectId(id))
    } yield {
      if (message.from != user.id && chat.userIds.contains(user.id)) messageService.read(oid)(chat).collect {
        case _ => Ok
      }.getOrElse(BadRequest) else Forbidden
    }).getOrElse(BadRequest)
  }

  /**
    * @api {POST} /api/message/deliverynew  Mark delivered
    * @apiName Mark delivered
    * @apiGroup Message
    * @apiParam {String}              message_id      Id of message which was readed.
    * @apiParamExample {json} Request-body:
    *                  {
    *                  "message_id": "dfdg9040mdfuhv8vdf7y6fd"
    *                  }
    * @apiDescription Notify server and users about delivered message. Only non-owners cat deliver message.
    */
  def deliverynew = authUtils.authenticateAction(parse.json) { request =>
    val json = request.body
    val user = request.user

    (for {
      id <- (json \ "message_id").asOpt[String] if ObjectId.isValid(id)
      oid = new ObjectId(id)
      chat <- messageService.findChatIdByObjectId(oid).flatMap { chatId =>
        chatService.findById(chatId)
      }
      message <- messageService.findById(new ObjectId(id))
    } yield {
      if (message.from != user.id && chat.userIds.contains(user.id)) messageService.delivery(oid)(chat).collect {
        case _ => Ok
      }.getOrElse(BadRequest) else Forbidden
    }).getOrElse(BadRequest)
  }

  /**
    * @api {POST} /api/message/change  Change message
    * @apiName Change message
    * @apiGroup Message
    * @apiParam {String}              id       Id of message which changing.
    * @apiParam {String}              message  New text of message.
    * @apiParamExample {json} Request-body:
    *                  {
    *                  "message": "haha, i change this message",
    *                  "id":"998sdfdfndfdsfd7fsdf"
    *                  }
    * @apiDescription Change message text. Only message owner (sender) can change message.
    */
  def change = authUtils.authenticateAction(parse.json) { request =>
    val json = request.body
    val user = request.user

    (for {
      groupId <- user.groupId
      id <- (json \ "id").asOpt[String] if ObjectId.isValid(id)
      oid = new ObjectId(id)
      key <- (json \ "key").asOpt[String].orElse(Some(""))
      text <- (json \ "message").asOpt[String]
      chatId <- messageService.findChatIdByObjectId(oid)
      chat <- chatService.findById(chatId)
      if messageService.change(oid, user.id, key, text)(groupId, chat).isUpdateOfExisting
    } yield {
      Ok
    }).getOrElse(BadRequest)
  }


  /**
    * @api {POST} /api/message/delete  Delete message
    * @apiName Delete message
    * @apiGroup Message
    * @apiParam {String}              id       Id of message which deleting.
    * @apiParam {Int}                 mode     (0 | 1 )Where 0 is soft delete, 1 - hard.
    * @apiParamExample {json} Request-body:
    *                  {
    *                  "mode": "0",
    *                  "id":"998sdfdfndfdsfd7fsdf"
    *                  }
    * @apiDescription Delete message. Only sender can delete message.
    */
  def delete = authUtils.authenticateAction(parse.json) { request =>
    val json = request.body
    val user = request.user

    (for {
      groupId <- user.groupId
      id <- (json \ "id").asOpt[String] if ObjectId.isValid(id)
      mode@(0 | 1) <- (json \ "mode").asOpt[Int]
      oid = new ObjectId(id)
      chatId <- messageService.findChatIdByObjectId(oid)
      chat <- chatService.findById(chatId)
      if (mode match {
        case 0 => messageService.softDelete(oid) _
        case 1 => messageService.remove(oid) _
      }) (groupId, chat).isUpdateOfExisting
    } yield {
      Ok
    }).getOrElse(BadRequest)
  }
}