package daos

import com.google.inject.{Inject, Singleton}
import com.mongodb.WriteConcern
import com.mongodb.casbah.commons.MongoDBObject
import models.{Message, MessageTime}
import org.bson.types.ObjectId
import ru.tochkak.plugin.salat.PlaySalat
import salat.dao.{ModelCompanion, SalatDAO}

@Singleton
class MessageDao @Inject()(
  mongoContext: MongoContext,
  playSalat: PlaySalat
) extends ModelCompanion[Message, ObjectId] {

  import mongoContext._

  val dao = new SalatDAO[Message, ObjectId](playSalat.collection("message", "ms")) {}

  dao.collection.createIndex(MongoDBObject("id" -> 1))
  dao.collection.createIndex(MongoDBObject("from" -> 1))
  dao.collection.createIndex(MongoDBObject("timestamp_post.timestamp" -> 1))
  dao.collection.createIndex(MongoDBObject("chat_id" -> 1))

  def all = {
    dao.find(MongoDBObject.empty).toList
  }

  def findById(id: ObjectId) = {
    dao.findOne(MongoDBObject("_id" -> id))
  }

  def findByChatId(chatId: Int, page: Int) = {
    dao.find(MongoDBObject(
      "chat_id" -> chatId
    ))
      .sort(MongoDBObject("timestamp_post.timestamp" -> -1))
      .skip(page * 100)
      .limit(100)
      .toList
  }

  def findUnreadMessages(id: Int, userId: Int) = {
    dao.find(MongoDBObject(
      "chat_id" -> id,
      "from" -> MongoDBObject("$ne" -> userId)
    )).filter(_.timestampRead.isEmpty).toList
  }

  def findChatIdByObjectId(id: ObjectId) = {
    dao.findOne(MongoDBObject(
      "_id" -> id
    )).map(_.chatId)
  }

  def findUnreadMessagesCount(id: Int, from: Int) = {
    dao.count(MongoDBObject(
      "chat_id" -> id,
      "timestamp_read" -> MongoDBObject("$exists" -> 0),
      "from" -> MongoDBObject("$ne" -> from)
    ))
  }

  def findUnreadMessagesCount(id: Int) = {
    dao.count(MongoDBObject(
      "chat_id" -> id,
      "timestamp_read" -> MongoDBObject("$exists" -> 0)
    ))
  }

  def findLastMessage(id: Int) = {
    dao.find(MongoDBObject("chat_id" -> id))
      .sort(MongoDBObject("timestamp_post.timestamp" -> -1))
      .limit(1)
      .toList
      .headOption
  }

  def change(oid: ObjectId, userId: Int, key: String, text: String) = {
    dao.update(
      MongoDBObject(
        "_id" -> oid,
        "from" -> userId
      ),
      MongoDBObject(
        "$set" -> MongoDBObject(
          "key" -> key,
          "text" -> text
        )
      )
    )
  }

  def softDelete(oid: ObjectId) = {
    dao.update(
      MongoDBObject("_id" -> oid),
      MongoDBObject(
        "$set" -> MongoDBObject(
          "deleted" -> true
        )
      )
    )
  }

  def markRead(oid: ObjectId) = {
    findById(oid).map(message =>
      dao.update(
        MongoDBObject("_id" -> oid),
        message.copy(timestampRead = Some(MessageTime()), timestampDelivery = Some(MessageTime())),
        upsert = false, multi = false, WriteConcern.ACKNOWLEDGED
      )
    )
  }

  def markDelivery(oid: ObjectId, chatId: Int) = {
    findById(oid).map(message =>
      dao.update(
        MongoDBObject("_id" -> oid),
        message.copy(timestampDelivery = Some(MessageTime())),
        upsert = false, multi = false, WriteConcern.ACKNOWLEDGED
      )
    )
  }
}
