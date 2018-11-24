package daos

import com.google.inject.{Inject, Singleton}
import com.mongodb.casbah.commons.MongoDBObject
import models.User
import org.bson.types.ObjectId
import ru.tochkak.plugin.salat.PlaySalat
import salat.dao.{ModelCompanion, SalatDAO}
import utils.Helper.StringExtended
import utils.MongoDbHelper.MongoDbCursorExtended

@Singleton
class UserDao @Inject()(
  mongoContext: MongoContext,
  playSalat: PlaySalat
) extends ModelCompanion[User, ObjectId] {

  import mongoContext._

  val dao = new SalatDAO[User, ObjectId](playSalat.collection("user", "ms")) {}

  {
    dao.find(MongoDBObject.empty).toList.foreach { user =>
      if (user.password.length != 32) {
        dao.update(
          MongoDBObject("_id" -> user._id),
          MongoDBObject("$set" -> MongoDBObject(
            "password" -> user.password.md5.md5.md5
          ))
        )
      }
    }
  }

  def maxId = {
    dao.find(MongoDBObject.empty)
      .sort(MongoDBObject("id" -> -1))
      .foldHeadO(0)(_.id)
  }

  def findOne(id: Int) = {
    dao.findOne(MongoDBObject("id" -> id))
  }

  def find(id: ObjectId, login: String, password: String) = {
    dao.findOne(MongoDBObject(
      "_id" -> id,
      "login" -> login,
      "password" -> password,
      "active" -> true,
      "archive" -> false
    ))
  }

  def all = findAll().toList

  def findByLoginAndPassword(login: String, password: String) = {
    dao.findOne(MongoDBObject(
      "login" -> login,
      "password" -> password,
      "archive" -> false
    ))
  }

  def setActive(user: User) = {
    dao.update(
      MongoDBObject("_id" -> user._id),
      MongoDBObject("$set" -> MongoDBObject("active" -> true))
    )
  }

  def setInactive(user: User) = {
    dao.update(
      MongoDBObject("_id" -> user._id),
      MongoDBObject("active" -> false)
    )
  }

  def findByLogin(login: String) = {
    dao.findOne(MongoDBObject(
      "login" -> login
    ))
  }

  def updateActivity(id: Int) = {
    dao.update(
      MongoDBObject("id" -> id),
      MongoDBObject(
        "$set" -> MongoDBObject(
          "last_activity" -> (System.currentTimeMillis() / 1000).toInt
        )
      )
    )
  }

  def updatePassword(id: Int, password: String) = {
    dao.update(
      MongoDBObject("id" -> id),
      MongoDBObject(
        "$set" -> MongoDBObject(
          "password" -> password
        )
      )
    )
  }

  def findByIdNonArchive(id: Int) = {
    dao.findOne(MongoDBObject(
      "id" -> id,
      "archive" -> false
    ))
  }

  def archive(id: Int) = {
    dao.update(
      MongoDBObject("id" -> id),
      MongoDBObject(
        "$set" -> MongoDBObject(
          "archive" -> true
        )
      )
    )
  }

  def findAllNonArchive(ids: List[Int], groupId: Int) = {
    dao.count(MongoDBObject(
      "id" -> MongoDBObject("$in" -> ids),
      "group_id" -> groupId
    ))
  }

  def findAllPossiblyOfflined = {
    dao.find(MongoDBObject(
      "last_activity" -> MongoDBObject("$lt" -> ((System.currentTimeMillis() / 1000).toInt - 600)),
      "active" -> true,
      "archive" -> false
    )).toList
  }

  def setStatus(userId: Int, status: String) = {
    dao.update(
      MongoDBObject("user_id" -> userId),
      MongoDBObject(
        "$set" -> MongoDBObject(
        "status" -> status
        )
      )
    )
  }

  def removeStatus(userId: Int) = {
    dao.update(
      MongoDBObject("user_id" -> userId),
      MongoDBObject(
        "$set" -> MongoDBObject(
          "status" -> ""
        )
      )
    )
  }

  def setAvatar(userId: Int, uuid: String) = {
    dao.update(
      MongoDBObject("user_id" -> userId),
      MongoDBObject(
        "$set" -> MongoDBObject(
          "avatar" -> uuid
        )
      )
    )
  }

  def removeAvatar(userId: Int) = {
    dao.update(
      MongoDBObject("user_id" -> userId),
      MongoDBObject(
        "$set" -> MongoDBObject(
          "avatar" -> ""
        )
      )
    )
  }
}
