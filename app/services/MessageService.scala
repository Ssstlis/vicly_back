package services

import com.google.inject.{Inject, Singleton}
import daos.MessageDao
import models.Message
import org.bson.types.ObjectId

@Singleton
class MessageService @Inject()(messageDao: MessageDao) {

  def all = messageDao.all

  def save(message: Message) = messageDao.dao.save(message)

  def findByChatId(chatId: Int) = messageDao.findByChatId(chatId)

  def markRead(id: ObjectId) = messageDao.markRead(id)

  def markDelivery(id: ObjectId) = messageDao.markDelivery(id)
}
