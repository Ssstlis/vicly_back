package daos

import com.google.inject.{Inject, Singleton}
import com.mongodb.casbah.commons.MongoDBObject
import models.User
import org.bson.types.ObjectId
import ru.tochkak.plugin.salat.PlaySalat
import salat.dao.{ModelCompanion, SalatDAO}
import utils.MongoDbHelper.MongoDbCursorExtended

@Singleton
class UserDao @Inject()(
  mongoContext: MongoContext,
  playSalat: PlaySalat
) extends ModelCompanion[User, ObjectId] {

  import mongoContext._

  val dao = new SalatDAO[User, ObjectId](playSalat.collection("user", "ms")){}

  def maxId = {
    dao.find(MongoDBObject.empty)
      .sort(MongoDBObject("id" -> 1))
      .foldHeadO(0)(_.id)
  }

  def findOne(id: Int) = {
    dao.findOne(MongoDBObject("id" -> id))
  }

  def all = findAll().toList

  def findByLoginAndPassword(login: String, password: String) = {
    dao.findOne(MongoDBObject(
      "login" -> login,
      "password" -> password
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
}
