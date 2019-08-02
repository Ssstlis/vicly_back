package services

import com.google.inject.{Inject, Singleton}
import daos.UserDao
import models.User
import org.bson.types.ObjectId
import pdi.jwt.JwtJson
import utils.Helper.StringExtended
import utils.CollectionHelper.TraversableOnceHelper

@Singleton
class UserService @Inject()(
  socketNotificationService: SocketNotificationService,
  groupService: GroupService,
  chatService: ChatService,
  messageService: MessageService,
  config: ConfigService,
  userDao: UserDao
) {

  def signup(user: User) = {
    findByLogin(user.login).map { user =>
      create(user).wasAcknowledged()
    }
  }

  def login(login: String, password: String) = {
    findByLoginAndPassword(login, password).map { user =>
      setActive(user)
      updateActivity(user.id)(user.groupId)
      (JwtJson.encode(user.claim, config.secret_key, config.algo), user)
    }
  }

  def logout(user: User): Unit = {
    socketNotificationService.offline(user.groupId, user.id)
    setInactive(user)
  }

  def listAll(user: User) = {
    val groups = groupService.all.zipBy(_.id)

    val users = all.map {
      case user if user.groupId.isDefined => Right(user)
      case user => Left(user)
    }.par

    val usersWithoutGroup = users.collect { case Left(user) => user }.toList

    val usersWithGroup = users
      .collect { case Right(user) => user }
      .groupBy(_.groupId)
      .map { case (groupIdO, users) =>
        (for {
          groupId <- groupIdO
          group <- groups.get(groupId)
        } yield {
          Right(group -> users)
        }).getOrElse(Left(users))
      }

    val withoutGroup = usersWithoutGroup ::: usersWithGroup.flatMap {
      case Left(users) => users
      case _ => List.empty[User]
    }.toList

    val withGroups = usersWithGroup.collect {
      case Right((group, users)) =>
        val usersWithMessages = users.seq.map { mUser =>
          val chat = chatService.findUserChat(mUser.id, user.id)
          val (unread, lastO) = chat.map { chat =>
            messageService.findUnreadMessagesCount(chat.id, user.id) ->
            messageService.findLastMessage(chat.id)
          }.getOrElse(0L, None)
          (mUser, unread, lastO, chat)
        }
        val groupChatMap = chatService.findGroupChatByGroupId(group.id)
          .filter(chat => chat.userIds.contains(user.id))
          .map(chat =>
            chat.id -> (messageService.findUnreadMessagesCount(chat.id), messageService.findLastMessage(chat.id), chat)
          ).toMap
        group -> (usersWithMessages, groupChatMap)
    }.toMap.seq
    (withoutGroup, withGroups)
  }

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

  def updatePassword(user: User, newPassword: String) = {
    if (userDao.updatePassword(user.id, newPassword.md5.md5.md5).isUpdateOfExisting) {
      findByLoginAndPassword(user.login, newPassword).map { user =>
        setActive(user)
        updateActivity(user.id)(user.groupId)
        JwtJson.encode(user.claim, config.secret_key, config.algo)
      }
    } else {
      None
    }
  }

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

  def setAvatar(userId: Int, attachment_id: ObjectId) = {
    val result = userDao.setAvatar(userId, attachment_id)
    if (result.isUpdateOfExisting) socketNotificationService.userSetAvatar(userId, attachment_id.toString)
    result
  }

  def removeAvatar(userId: Int)(groupId: Int) = {
    val result = userDao.removeAvatar(userId)
    if (result.isUpdateOfExisting) socketNotificationService.userRemoveAvatar(groupId, userId)
    result
  }

  def roleCount(id: Int, groupId: Int) = userDao.roleCount(id, groupId)
}