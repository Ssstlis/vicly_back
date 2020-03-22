package services

import com.google.inject.{Inject, Singleton}
import daos.InviteDao
import models.Invite

@Singleton
class InviteService @Inject()(inviteDao: InviteDao) {

  def create(invite: Invite) = inviteDao.save(invite)

  def remove(invite: Invite) = inviteDao.remove(invite)

  def find(uuid: String) = inviteDao.find(uuid)

  def all = inviteDao.all

}
