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

  def findGroupChats(userId: Int) = {
    dao.find(MongoDBObject("user_ids" -> userId)).toList
  }

  def findGroupChatByGroupId(groupId: Int) = {
    dao.find(MongoDBObject(
      "group_id" -> groupId,
      "chat_type" -> "group"
    )).toList
  }

  def findUserChat(first: Int, second: Int) = {
    dao.findOne(MongoDBObject(
      "$and" -> MongoDBList(
        "user_ids" -> MongoDBObject("$all" -> MongoDBList(first, second)),
        "$or" -> MongoDBList(
          "user_ids" -> MongoDBObject("$size" -> 2),
          "chat_type" -> "user"
        )
      )
    ))
  }

  def findUserChat(userId: Int) = {
    dao.findOne(MongoDBObject(
      "$and" -> MongoDBList(
        "user_ids" -> MongoDBObject("$all" -> MongoDBList(userId)),
        "$or" -> MongoDBList(
          "user_ids" -> MongoDBObject("$size" -> 2),
          "chat_type" -> "user"
        )
      )
    ))
  }

  def createUserChat(first: Int, second: Int, groupId: Int) = {
    dao.insert(Chat(maxId + 1, groupId, List(first, second), "user", None, None)).isDefined
  }

  def createGroupChat(userIds: List[Int], groupId: Int, ownerId: ObjectId, name: String, purpose: Option[String] = None) = {
    dao.insert(Chat(maxId + 1, groupId, userIds, "group", Some(false), Some(ownerId), Some(name), purpose)).isDefined
  }

  def findGroupChat(id: Int, groupId: Int) = {
    dao.findOne(MongoDBObject(
      "id" -> id,
      "chat_type" -> "group",
      "group_id" -> groupId,
      "archive" -> false
    ))
  }

  def findByChatAndGroupId(chatId: Int, groupId: Int) = {
    dao.findOne(MongoDBObject(
      "chat_id" -> chatId,
      "group_id" -> groupId,
      "$or" -> MongoDBList(
        "$and" -> MongoDBList(
          "chat_type" -> "group",
          "archive" -> false,
        ),
        "archive" -> false
      )
    ))
  }

  def updateUsers(chatId: Int, userIds: List[Int]) = {
    dao.update(
      MongoDBObject("id" -> chatId),
      MongoDBObject(
        "$set" -> MongoDBObject(
          "user_ids" -> userIds
        )
      )
    )
  }

  def archive(chatId: Int) = {
    dao.update(
      MongoDBObject("id" -> chatId),
      MongoDBObject(
        "$set" -> MongoDBObject(
          "archive" -> true
        )
      )
    )
  }
}
