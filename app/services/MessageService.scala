package services

import com.google.inject.{Inject, Singleton}
import daos.MessageDao
import models.Message

@Singleton
class MessageService @Inject()(messageDao: MessageDao) {

  def all = messageDao.all

  def add(message: Message) = messageDao.dao.save(message)
}
