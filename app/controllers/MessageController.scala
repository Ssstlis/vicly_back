package controllers

import actions.AuthUtils
import com.google.inject.{Inject, Singleton}
import models.Message
import org.bson.types.ObjectId
import play.api.libs.json.Json
import play.api.mvc.InjectedController
import services.{ChatService, MessageService, UserService}

@Singleton
class MessageController @Inject()(
  authUtils: AuthUtils,
  chatService: ChatService,
  messageService: MessageService,
  userService: UserService
) extends InjectedController {

  def postMessage = authUtils.authenticateAction(parse.json) { request =>
    val user = request.user
    (for {
      groupId <- user.groupId
      message <- request.body.asOpt(Message.reads(user.id))
      chatType@("user" | "group") <- (request.body \ "type").asOpt[String]
      targetUserId = message.chatId
    } yield {
      chatType match {
        case "user" => {
          if (userService.findByIdNonArchive(message.chatId).isEmpty) {
            chatService.findUserChat(user.id, targetUserId).map { chat =>
              if (
                userService.findOne(message.chatId).isDefined &&
                messageService.save(message.copy(chatId = chat.id)).wasAcknowledged()
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
                chatService.findUserChat(user.id, targetUserId).map { chat =>
                  if (messageService.save(message.copy(chatId = chat.id)).wasAcknowledged()) {
                    Ok
                  } else {
                    BadRequest
                  }
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
          chatService.findGroupChat(targetUserId, groupId).collect {
            case chat if messageService.save(message.copy(chatId = chat.id)).wasAcknowledged() => Ok
          }.getOrElse(BadRequest)
      }
    }).getOrElse(BadRequest)
  }

  def chatMessages(chatId: Int, chatType: String, page: Int) = authUtils.authenticateAction { request =>
    val user = request.user
    val messages = (chatType match {
      case "user" => chatService.findUserChat(user.id, chatId)
      case "group" => chatService.findById(chatId)
      case _ => None
    }).map { chat =>
      messageService.findByChatId(chat.id, page)
    }.getOrElse(List.empty)
    Ok(Json.toJson(messages))
  }

  def undeadMessages(chatId: Int, chatType: String) = authUtils.authenticateAction { request =>
    val user = request.user
    val messages = (chatType match {
      case "user" => chatService.findUserChat(user.id, chatId)
      case "group" => chatService.findById(chatId)
      case _ => None
    }).map { chat =>
      messageService.findUnreadMessages(chat.id)
    }.getOrElse(List.empty)
    Ok(Json.toJson(messages))
  }

  def readMessage = authUtils.authenticateAction(parse.json) { request =>
    val json = request.body
    val user = request.user

    (for {
      groupId <- user.groupId
      id <- (json \ "id").asOpt[String] if ObjectId.isValid(id)
      oid = new ObjectId(id)
      chatId <- (json \ "chat_id").asOpt[Int]
      _ <- (request.body \ "type").asOpt[String].map {
        case "user" => {
          messageService.findChatIdByObjectId(oid).flatMap(chatId =>
            chatService.findById(chatId)
          )
        }
        case "group" => chatService.findGroupChat(chatId, groupId)
        case _ => None
      }
      result <- messageService.read(oid, chatId) if result.isUpdateOfExisting
    } yield {
      Ok
    }).getOrElse(BadRequest)
  }

  def deliveryMessage = authUtils.authenticateAction(parse.json) { request =>
    val json = request.body
    val user = request.user

    (for {
      groupId <- user.groupId
      id <- (json \ "id").asOpt[String] if ObjectId.isValid(id)
      oid = new ObjectId(id)
      chatId <- (json \ "chat_id").asOpt[Int]
      _ <- (request.body \ "type").asOpt[String].map {
        case "user" => {
          messageService.findChatIdByObjectId(oid).flatMap(chatId =>
            chatService.findById(chatId)
          )
        }
        case "group" => chatService.findGroupChat(chatId, groupId)
        case _ => None
      }
      result <- messageService.delivery(oid, chatId) if result.isUpdateOfExisting
    } yield {
      Ok
    }).getOrElse(BadRequest)
  }

  def change = authUtils.authenticateAction(parse.json) { request =>
    val json = request.body
    val user = request.user

    (for {
      id <- (json \ "id").asOpt[String] if ObjectId.isValid(id)
      oid = new ObjectId(id)
      key <- (json \ "key").asOpt[String].orElse(Some(""))
      text <- (json \ "message").asOpt[String]
      if messageService.change(oid, user.id, key, text).isUpdateOfExisting
    } yield {
      Ok
    }).getOrElse(BadRequest)
  }

  def delete = authUtils.authenticateAction(parse.json) { request =>
    val json = request.body

    (for {
      id <- (json \ "id").asOpt[String] if ObjectId.isValid(id)
      mode@(0 | 1) <- (json \ "mode").asOpt[Int]
      oid = new ObjectId(id)
      if (mode match {
        case 0 => messageService.softDelete(oid)
        case 1 => messageService.remove(oid)
      }).isUpdateOfExisting
    } yield {
      Ok
    }).getOrElse(BadRequest)
  }
}