package services

import com.google.inject.{Inject, Singleton}
import daos.ChatDao

@Singleton
class ChatService @Inject()(chatDao: ChatDao) {

  def all = chatDao.all

  def findByIds(ids: List[Int]) = chatDao.findByIds(ids)
}
