package services

import com.google.inject.{Inject, Singleton}
import daos.MessageDao
import models.{Chat, Message, User}
import org.bson.types.ObjectId
import play.api.libs.json.Json
import utils.Helper.StringExtended

@Singleton
class MessageService @Inject()(
  messageDao: MessageDao,
  chatService: ChatService,
  attachmentService: AttachmentService,
  socketNotificationService: SocketNotificationService
)(implicit configService: ConfigService) {

  implicit class MessageExtended(m: Message) {
    def encode = m.copy(text = m.text.encodeToken)

    def decode = {
      for {
        json <- m.text.decodeToken
        text <- (json \ "text").asOpt[String]
      } yield m.copy(text = text)
    }
  }

  def sendMessageInChat(message: Message)(implicit user: User) = {
    chatService.findGroupChatWithUser(user.id, message.chatId).map { case chat =>
      val filledMessage = message.copy(chatId = chat.id, replyForO = message.replyForO)
      save(filledMessage)(chat).wasAcknowledged()
    }
  }

  def getGroupChatMessages(chatId: Int, page: Int) = {
    chatService.findById(chatId).map { chat =>
      findByChatId(chat.id, page)
    }.getOrElse(List.empty).sortBy(mess => mess._1.timestampPost.timestamp)
  }

  def getUserChatMessages(userId: Int, page: Int)(implicit user: User) = {
    chatService.findUserChat(user.id, userId).map { chat =>
      findByChatId(chat.id, page)
    }.getOrElse(List.empty).sortBy(mess => mess._1.timestampPost.timestamp)
  }

  def getUserChatMessagesFrom(userId: Int, messageId: String)(implicit user: User): Either[String, List[Message]] = {
    chatService.findUserChat(user.id, userId).map { chat =>
      Right(findMessagesAfter(chat.id, new ObjectId(messageId)))
    }.getOrElse(Left("Can't find chat with user $userId"))
  }

  def all = messageDao.all.flatMap(_.decode)

  def save(message: Message)(chat: Chat) = {
    val result = messageDao.dao.save(message.encode)
    if (result.wasAcknowledged()) {
      socketNotificationService.newMessage(message, chat)
    }
    result
  }

  def findByChatId(chatId: Int, page: Int) = {
    messageDao.findByChatId(chatId, page: Int)
      .flatMap(_.decode)
      .map { message =>
        (message, getMessageAttachments(message))
      }
  }

  def getMessageAttachments(message: Message) = {
    message.attachments
      .flatMap { attachmentId =>
        attachmentService.findById(attachmentId)
      }
  }

  def findById(id: ObjectId) = messageDao.findById(id).flatMap(_.decode)

  def read(id: ObjectId)(chat: Chat) = {
    val result = messageDao.markRead(id)
    result match {
      case Some(message) => socketNotificationService.markRead(id, chat, message)
      case _ =>
    }
    result
  }

  def delivery(id: ObjectId)(chat: Chat) = {
    val result = messageDao.markDelivery(id)
    result match {
      case Some(message) => socketNotificationService.markDelivery(id, chat, message)
      case _ =>
    }
    result
  }

  def findUnreadMessages(id: Int, userId: Int) = messageDao.findUnreadMessages(id, userId).flatMap(_.decode)

  def findUnreadMessagesCount(id: Int, from: Int) = messageDao.findUnreadMessagesCount(id, from)

  def findUnreadMessagesCount(id: Int) = messageDao.findUnreadMessagesCount(id)

  def findLastMessage(id: Int) = messageDao.findLastMessage(id).flatMap(_.decode)

  def change(id: ObjectId, userId: Int, key: String, text: String)(groupId: Int, chat: Chat) = {
    val result = messageDao.change(id, userId, key, text.encodeToken)
    if (result.isUpdateOfExisting) socketNotificationService.changed(groupId, id, chat, chat.userIds)
    result
  }

  def softDelete(id: ObjectId)(groupId: Int, chat: Chat) = {
    val result = messageDao.softDelete(id)
    if (result.isUpdateOfExisting) {
      socketNotificationService.softDelete(groupId, id, chat, chat.userIds)
      Some(result)
    } else None
  }

  def remove(id: ObjectId)(groupId: Int, chat: Chat) = {
    val result = messageDao.removeById(id)
    if (result.wasAcknowledged()) {
      socketNotificationService.remove(groupId, id, chat, chat.userIds)
      Some(result)
    } else None
  }

  def findChatIdByObjectId(id: ObjectId) = messageDao.findChatIdByObjectId(id)

  def findMessagesAfter(chatId: Int, messageId: ObjectId) = messageDao.findMessagesAfter(chatId, messageId)
}
