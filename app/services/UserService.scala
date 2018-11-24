package services

import com.google.inject.{Inject, Singleton}
import daos.UserDao
import models.User
import org.bson.types.ObjectId
import utils.Helper.StringExtended

@Singleton
class UserService @Inject()(userDao: UserDao) {

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

  def updateActivity(id: Int) = userDao.updateActivity(id)

  def updatePassword(id: Int, password: String) = userDao.updatePassword(id, password.md5.md5.md5)

  def findByIdNonArchive(id: Int) = userDao.findByIdNonArchive(id)

  def archive(id: Int) = userDao.archive(id)


}