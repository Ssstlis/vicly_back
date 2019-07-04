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

  def postnewuser = authUtils.authenticateAction(parse.json) { request =>
    val user = request.user
    val json = request.body

    (for {
      groupId <- user.groupId
      message <- json.asOpt(Message.reads(user.id))
    } yield {
      val replyForO = (json \ "reply_for").asOpt[ObjectId]
      val targetUserId = message.chatId
      if (targetUserId != 0 && userService.findByIdNonArchive(targetUserId).isDefined) {
        chatService.findUserChat(user.id, targetUserId).map { chat =>
          if (
            userService.findOne(targetUserId).isDefined && {
              val filledMessage = message.copy(chatId = chat.id, replyForO = replyForO)
              messageService.save(filledMessage)(chat).wasAcknowledged()
            }
          ) {
            Ok
          } else {
            ResetContent
          }
        }.getOrElse {
          if (
            userService.findOne(targetUserId).isDefined &&
              chatService.createUserChat(user.id, targetUserId, groupId)
          ) {
            chatService.findUserChat(user.id, targetUserId).collect { case chat
              if {
                val filledMessage = message.copy(chatId = chat.id, replyForO = replyForO)
                messageService.save(filledMessage)(chat).wasAcknowledged()
              } => Ok
            }.getOrElse(BadRequest)
          } else {
            BadRequest
          }
        }
      } else {
        BadRequest
      }
    }).getOrElse(BadRequest)
  }

  def getGroupChatMessages(chatId: Int, page: Int) = authUtils.authenticateAction { _ =>
    Ok(Json.toJson(messageService.getGroupChatMessages(chatId: Int, page: Int)))
  }

  def getUserChatMessages(userId: Int, page: Int) = authUtils.authenticateAction { request =>
    implicit val user = request.user
    Ok(Json.toJson(messageService.getUserChatMessages(userId, page)))
  }

  def getUserChatMessagesFrom(userId: Int, messageId: String) = authUtils.authenticateAction { request =>
    implicit val user = request.user
    messageService.getUserChatMessagesFrom(userId, messageId) match {
      case Right(messages) => Ok(Json.toJson(messages))
      case Left(error) => BadRequest(Json.obj("error" -> error))
    }
  }

  def groupChatMessagesFrom(chatId: Int, messageId: String) = authUtils.authenticateAction { request =>
    chatService.findGroupChat(chatId).map { chat =>
      Ok(Json.toJson(messageService.findMessagesAfter(chat.id, new ObjectId(messageId))))
    }.getOrElse(BadRequest(Json.obj("error" -> s"Can't find group chat with id $chatId")))
  }

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
      if (message.from != user.id && chat.userIds.contains(user.id)) {
        messageService.read(oid)(chat).collect {
          case _ => Ok
        }.getOrElse(BadRequest)
      } else {
        Forbidden
      }
    }).getOrElse(BadRequest)
  }

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
      if (message.from != user.id && chat.userIds.contains(user.id)) {
        messageService.delivery(oid)(chat).collect {
          case _ => Ok
        }.getOrElse(BadRequest)
      } else {
        Forbidden
      }
    }).getOrElse(BadRequest)
  }

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