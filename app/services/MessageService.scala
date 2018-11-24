package services

import com.google.inject.{Inject, Singleton}
import daos.MessageDao
import models.{Chat, Message}
import org.bson.types.ObjectId
import play.api.libs.json.Json
import utils.JsonHelper.ObjectIdFormat

@Singleton
class MessageService @Inject()(
  messageDao: MessageDao,
  socketNotificationService: SocketNotificationService
) {

  def all = messageDao.all

  def save(message: Message)(groupId: Int, chatType: String, chat: Chat) = {
    val result = messageDao.dao.save(message)
    if (result.wasAcknowledged()) {
      socketNotificationService.newMessage(groupId, chatType, message, chat)
    }
    result
  }

  def findByChatId(chatId: Int, page: Int) = messageDao.findByChatId(chatId, page: Int)

  def read(id: ObjectId, chatId: Int)(groupId: Int) = {
    val result = messageDao.markRead(id, chatId)
    result.foreach(result => if (result.isUpdateOfExisting) {
      socketNotificationService.markRead(groupId, Json.obj("id" -> id))
    })
    result
  }

  def delivery(id: ObjectId, chatId: Int)(groupId: Int) = {
    val result = messageDao.markDelivery(id, chatId)
    result.foreach(result => if (result.isUpdateOfExisting) {
      socketNotificationService.markDelivery(groupId, Json.obj("id" -> id))
    })
    result
  }

  def findUnreadMessages(id: Int) = messageDao.findUnreadMessages(id)

  def findUnreadMessagesCount(id: Int, from: Int) = messageDao.findUnreadMessagesCount(id, from)

  def findUnreadMessagesCount(id: Int) = messageDao.findUnreadMessagesCount(id)

  def findLastMessage(id: Int, from: Int) = messageDao.findLastMessage(id, from)

  def findLastMessage(id: Int) = messageDao.findLastMessage(id)

  def change(id: ObjectId, userId: Int, key: String, text: String)(groupId: Int) = {
    val result = messageDao.change(id, userId, key, text)
    if (result.isUpdateOfExisting) {
      socketNotificationService.changed(groupId, Json.obj("id" -> id))
    }
    result
  }

  def softDelete(id: ObjectId)(groupId: Int) = {
    val result = messageDao.softDelete(id)
    if (result.isUpdateOfExisting) {
      socketNotificationService.softDelete(groupId, Json.obj("id" -> id))
    }
    result
  }

  def remove(id: ObjectId)(groupId: Int) = {
    val result = messageDao.removeById(id)
    if (result.isUpdateOfExisting) {
      socketNotificationService.remove(groupId, Json.obj("id" -> id))
    }
    result
  }

  def findChatIdByObjectId(id: ObjectId) = messageDao.findChatIdByObjectId(id)
}
