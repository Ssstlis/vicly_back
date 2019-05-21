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

  val dao = new SalatDAO[Chat, ObjectId](playSalat.collection("chat", "ms")) {}

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

  def findGroupChatWithUser(userId: Int, chatId: Int) = {
    dao.findOne(
      MongoDBObject(
        "user_ids" -> MongoDBObject("$in" -> MongoDBList(userId)),
        "chat_type" -> "group",
        "id" -> chatId
      )
    )
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
        "$or" -> MongoDBList(
          MongoDBObject("user_ids" -> MongoDBList(first, second)),
          MongoDBObject("user_ids" -> MongoDBList(second, first)),
        ),
        "$or" -> MongoDBList(
          "user_ids" -> MongoDBObject("$size" -> 2),
          "chat_type" -> "user"
        )
      )
    ))
  }

  def createUserChat(first: Int, second: Int) = {
    val chat = Chat(maxId + 1, None, List(first, second), "user", None, None)
    dao.insert(chat).isDefined
  }

  def createGroupChat(userIds: List[Int], groupId: Option[Int] = None, ownerId: ObjectId, name: String, purpose: Option[String] = None) = {
    val chat = Chat(maxId + 1, groupId, userIds, "group", Some(false), Some(ownerId), Some(name), purpose)
    dao.insert(chat).flatMap(chatId => {
      dao.findOneById(chatId)
    })
  }

  def findGroupChat(id: Int) = {
    dao.findOne(MongoDBObject(
      "id" -> id,
      "chat_type" -> "group",
      "archive" -> false
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
