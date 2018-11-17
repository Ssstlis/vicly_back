package services

import com.google.inject.{Inject, Singleton}
import daos.GroupDao
import models.Group

@Singleton
class GroupService @Inject()(groupDao: GroupDao) {

  def all = groupDao.all

  def maxId = groupDao.maxId

  def nextId = maxId + 1

  def create(group: Group) = groupDao.create(group)

  def findById(id: Int) = groupDao.findById(id)

  def setPurpose(id: Int, purpose: String) = groupDao.setPurpose(id, purpose)
}
