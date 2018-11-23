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
    )).skip(page * 20).limit(20).toList
  }

  def markRead(id: ObjectId) = {
    findById(id).map(message =>
      dao.update(
        MongoDBObject("_id" -> id),
        message.copy(timestampRead = Some(MessageTime())),
        upsert = false, multi = false, WriteConcern.ACKNOWLEDGED
      )
    )
  }

  def markDelivery(id: ObjectId) = {
    findById(id).map(message =>
      dao.update(
        MongoDBObject("_id" -> id),
        message.copy(timestampDelivery = Some(MessageTime())),
        upsert = false, multi = false, WriteConcern.ACKNOWLEDGED
      )
    )
  }

  def findUnreadMessages(id: Int) = {
    dao.find(MongoDBObject(
      "chat_id" -> id,
      "timestamp_read" -> MongoDBObject("$exists" -> 0)
    )).toList
  }
}
