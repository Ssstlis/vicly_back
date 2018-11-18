package daos

import com.google.inject.{Inject, Singleton}
import com.mongodb.casbah.commons.{MongoDBList, MongoDBObject}
import models.Chat
import org.bson.types.ObjectId
import ru.tochkak.plugin.salat.PlaySalat
import salat.dao.{ModelCompanion, SalatDAO}
import utils.MongoDbHelper.MongoDbCursorExtended

@Singleton
class ChatDao @Inject()(
  mongoContext: MongoContext,
  playSalat: PlaySalat
) extends ModelCompanion[Chat, ObjectId] {

  import mongoContext._

  val dao = new SalatDAO[Chat, ObjectId](playSalat.collection("chat", "ms")){}

  def all = dao.find(MongoDBObject.empty).toList

  def maxId = {
    dao.find(MongoDBObject.empty)
      .sort(MongoDBObject("id" -> -1))
      .foldHeadO(0)(_.id)
  }

  def findByIds(ids: List[Int]) = {
    dao.find(MongoDBObject(
      "id" -> MongoDBObject("$in" -> ids)
    )).toList
  }

  def findById(id: Int) = {
    dao.findOne(MongoDBObject("id" -> id))
  }

  def findUserGroupChats(userId: Int) = {
    dao.find(MongoDBObject("user_ids" -> userId)).toList
  }

  def findUserChat(first: Int, second: Int) = {
    dao.findOne(MongoDBObject(
      "$and" -> MongoDBList(
        "user_ids" -> MongoDBList(first, second),
        "user_ids" -> MongoDBObject("$size" -> 2)
      )
    ))
  }

  def findUserChat(userId: Int) = {
    dao.findOne(MongoDBObject(
      "$and" -> MongoDBList(
        "user_ids" -> userId,
        "user_ids" -> MongoDBObject("$size" -> 2)
      )
    ))
  }

  def createChat(first: Int, second: Int, groupId: Int) = {
    dao.insert(Chat(maxId + 1, groupId, List(first, second))).isDefined
  }
}
