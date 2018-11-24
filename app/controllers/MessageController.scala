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

  def post = authUtils.authenticateAction(parse.json) { request =>
    val user = request.user
    (for {
      groupId <- user.groupId
      message <- request.body.asOpt(Message.reads(user.id))
      chatType@("user" | "group") <- (request.body \ "type").asOpt[String]
    } yield {
      val targetUserId = message.chatId
      val replyForO = (request.body \ "reply_for").asOpt[ObjectId]
      chatType match {
        case "user" => {
          if (userService.findByIdNonArchive(targetUserId).isDefined) {
            chatService.findUserChat(user.id, targetUserId).map { chat =>
              if (
                userService.findOne(targetUserId).isDefined && {
                  val filledMessage = message.copy(chatId = chat.id, replyForO = replyForO)
                  messageService.save(filledMessage)(groupId, "user", chat).wasAcknowledged()
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
                    messageService.save(filledMessage)(groupId, "user", chat).wasAcknowledged()
                  } => Ok
                }.getOrElse(BadRequest)
              } else {
                BadRequest
              }
            }
          } else {
            BadRequest
          }
        }
        case "group" =>
          chatService.findGroupChat(targetUserId, groupId).collect { case chat
            if {
              val filledMessage = message.copy(chatId = chat.id, replyForO = replyForO)
              messageService.save(filledMessage)(groupId, "user", chat).wasAcknowledged()
            } => Ok
          }.getOrElse(BadRequest)
      }
    }).getOrElse(BadRequest)
  }

  def chat(chatId: Int, chatType: String, page: Int) = authUtils.authenticateAction { request =>
    val user = request.user
    val messages = (chatType match {
      case "user" => chatService.findUserChat(user.id, chatId)
      case "group" => chatService.findById(chatId)
      case _ => None
    }).map { chat =>
      messageService.findByChatId(chat.id, page)
    }.getOrElse(List.empty).sortBy(_.timestampPost.timestamp)
    Ok(Json.toJson(messages))
  }

  def undead(chatId: Int, chatType: String) = authUtils.authenticateAction { request =>
    val user = request.user
    val messages = (chatType match {
      case "user" => chatService.findUserChat(user.id, chatId)
      case "group" => chatService.findById(chatId)
      case _ => None
    }).map { chat =>
      messageService.findUnreadMessages(chat.id, user.id)
    }.getOrElse(List.empty).sortBy(_.timestampPost.timestamp)
    Ok(Json.toJson(messages))
  }

  def read = authUtils.authenticateAction(parse.json) { request =>
    val json = request.body
    val user = request.user

    (for {
      groupId <- user.groupId
      id <- (json \ "id").asOpt[String] if ObjectId.isValid(id)
      oid = new ObjectId(id)
      chatId <- (json \ "chat_id").asOpt[Int]
      chat <- (json \ "chat_type").asOpt[String].flatMap {
        case "user" => {
          messageService.findChatIdByObjectId(oid).flatMap { chatId =>
            chatService.findById(chatId)
          }
        }
        case "group" => chatService.findGroupChat(chatId, groupId)
        case _ => None
      }
      result <- messageService.read(oid)(groupId, chat) if result.isUpdateOfExisting
    } yield {
      Ok
    }).getOrElse(BadRequest)
  }

  def delivery = authUtils.authenticateAction(parse.json) { request =>
    val json = request.body
    val user = request.user

    (for {
      groupId <- user.groupId
      id <- (json \ "id").asOpt[String] if ObjectId.isValid(id)
      oid = new ObjectId(id)
      chatId <- (json \ "chat_id").asOpt[Int]
      chat <- (json \ "chat_type").asOpt[String].flatMap {
        case "user" => {
          messageService.findChatIdByObjectId(oid).flatMap(chatId =>
            chatService.findById(chatId)
          )
        }
        case "group" => chatService.findGroupChat(chatId, groupId)
        case _ => None
      }
      result <- messageService.delivery(oid, chatId)(groupId, chat) if result.isUpdateOfExisting
    } yield {
      Ok
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
      })(groupId, chat).isUpdateOfExisting
    } yield {
      Ok
    }).getOrElse(BadRequest)
  }
}