package services

import com.google.inject.{Inject, Singleton}
import daos.ChatDao
import org.bson.types.ObjectId

@Singleton
class ChatService @Inject()(chatDao: ChatDao) {

  def all = chatDao.all

  def findByIds(ids: List[Int]) = chatDao.findByIds(ids)

  def findById(id: Int) = chatDao.findById(id)

  def findGroupChats(userId: Int) = chatDao.findGroupChats(userId)

  def findGroupChatByGroupId(groupId: Int) = chatDao.findGroupChatByGroupId(groupId)

  def findUserChat(first: Int, second: Int) = chatDao.findUserChat(first, second)

  def findUserChatByUserId(userId: Int) = chatDao.findUserChat(userId)

  def createUserChat(first: Int, second: Int, groupId: Int) = chatDao.createUserChat(first, second, groupId)

  def createGroupChat(userIds: List[Int], groupId: Int, ownerId: ObjectId) = chatDao.createGroupChat(userIds, groupId, ownerId)

  def findGroupChat(id: Int, groupId: Int) = chatDao.findGroupChat(id, groupId)

  def findByChatAndGroupId(chatId: Int, groupId: Int) = chatDao.findByChatAndGroupId(chatId, groupId)
}
