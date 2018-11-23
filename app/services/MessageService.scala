package services

import com.google.inject.{Inject, Singleton}
import daos.MessageDao
import models.Message
import org.bson.types.ObjectId

@Singleton
class MessageService @Inject()(messageDao: MessageDao) {

  def all = messageDao.all

  def save(message: Message) = messageDao.dao.save(message)

  def findByChatId(chatId: Int, page: Int) = messageDao.findByChatId(chatId, page: Int)

  def read(id: ObjectId, chatId: Int) = messageDao.markRead(id, chatId)

  def delivery(id: ObjectId, chatId: Int) = messageDao.markDelivery(id, chatId)

  def findUnreadMessages(id: Int) = messageDao.findUnreadMessages(id)

  def findUnreadMessagesCount(id: Int, from: Int) = messageDao.findUnreadMessagesCount(id, from)

  def findUnreadMessagesCount(id: Int) = messageDao.findUnreadMessagesCount(id)

  def findLastMessage(id: Int, from: Int) = messageDao.findLastMessage(id, from)

  def findLastMessage(id: Int) = messageDao.findLastMessage(id)

  def change(oid: ObjectId, userId: Int, key: String, text: String) = messageDao.change(oid, userId, key, text)

  def softDelete(oid: ObjectId) = messageDao.softDelete(oid)

  def remove(oid: ObjectId) = messageDao.removeById(oid)

  def findChatIdByObjectId(id: ObjectId) = messageDao.findChatIdByObjectId(id)
}
