package services

import com.google.inject.{Inject, Singleton}
import daos.UserDao
import models.User
import org.bson.types.ObjectId

@Singleton
class UserService @Inject()(userDao: UserDao) {

  def findOne(id: Int) = userDao.findOne(id)

  def findByLoginAndPassword(login: String, password: String) = {
    userDao.findByLoginAndPassword(login, password)
  }

  def setActive(user: User) = userDao.setActive(user)

  def setInactive(user: User) = userDao.setInactive(user)

  def find(id: ObjectId) = userDao.findOneById(id)
}
