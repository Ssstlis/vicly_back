package services

import com.google.inject.{Inject, Singleton}
import daos.UserDao
import models.User
import org.bson.types.ObjectId
import utils.Helper.StringExtended

@Singleton
class UserService @Inject()(
  socketNotificationService: SocketNotificationService,
  userDao: UserDao
) {

  def maxId = userDao.maxId

  def nextId = maxId + 1

  def findOne(id: Int) = userDao.findOne(id)

  def findByLoginAndPassword(login: String, password: String) = {
    userDao.findByLoginAndPassword(login, password.md5.md5.md5)
  }

  def setActive(user: User) = userDao.setActive(user)

  def setInactive(user: User) = userDao.setInactive(user)

  def find(id: ObjectId, login: String, password: String) = userDao.find(id, login, password)

  def findByLogin(login: String) = userDao.findByLogin(login)

  def create(user: User) = userDao.dao.save(user)

  def all = userDao.all

  def save(user: User) = userDao.save(user)

  def updateActivity(id: Int)(groupIdO: Option[Int]) = {
    socketNotificationService.online(groupIdO, id)
    userDao.updateActivity(id)
  }

  def updatePassword(id: Int, password: String) = userDao.updatePassword(id, password.md5.md5.md5)

  def findByIdNonArchive(id: Int) = userDao.findByIdNonArchive(id)

  def usersSameGroupNonArchive(ids: List[Int], groupId: Int) = {
    ids.distinct.size.toLong == userDao.findAllNonArchive(ids, groupId)
  }

  def archive(id: Int)(groupIdO: Option[Int]) = {
    val result = userDao.archive(id)
    if (result.isUpdateOfExisting) socketNotificationService.archive(groupIdO, id)
    result
  }

  def findAllPossiblyOfflined = userDao.findAllPossiblyOfflined

  def setStatus(userId: Int, status: String)(groupId: Int) = {
    val result = userDao.setStatus(userId, status)
    if (result.isUpdateOfExisting) socketNotificationService.userSetStatus(groupId, userId, status)
    result
  }

  def removeStatus(userId: Int)(groupId: Int) = {
    val result = userDao.removeStatus(userId)
    if (result.isUpdateOfExisting) socketNotificationService.userRemoveStatus(groupId, userId)
    result
  }

  def setAvatar(userId: Int, uuid: String)(groupId: Int) = {
    val result = userDao.setAvatar(userId, uuid)
    if (result.isUpdateOfExisting) socketNotificationService.userSetAvatar(groupId, userId, uuid)
    result
  }

  def removeAvatar(userId: Int)(groupId: Int) = {
    val result = userDao.removeAvatar(userId)
    if (result.isUpdateOfExisting) socketNotificationService.userRemoveAvatar(groupId, userId)
    result
  }
}