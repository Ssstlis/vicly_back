package services

import com.google.inject.{Inject, Singleton}
import daos.RoleDao

@Singleton
class RoleService @Inject()(roleDao: RoleDao) {

  def find(id: Int, groupId: Int) = roleDao.find(id, groupId)

  def find(id: Int) = roleDao.find(id)

  def findGroupRoles(groupId: Int) = roleDao.findGroupRoles(groupId)

  def save(description: String, groupId: Int) = roleDao.save(description, groupId)

  def update(id: Int, description: String, groupId: Int) = roleDao.update(id, description, groupId)

  def remove(id: Int) = roleDao.remove(id)
}
