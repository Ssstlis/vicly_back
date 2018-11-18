package daos

import com.google.inject.{Inject, Singleton}
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

  def findByChatId(chatId: Int) = {
    dao.find(MongoDBObject(
      "chat_id" -> chatId
    )).toList
  }

  def markRead(id: ObjectId) = {
    dao.update(
      MongoDBObject("_id" -> id),
      MongoDBObject("$set" -> MongoDBObject(
        "timestamp_read" -> MessageTime()
      ))
    )
  }

  def markDelivery(id: ObjectId) = {
    dao.update(
      MongoDBObject("_id" -> id),
      MongoDBObject("$set" -> MongoDBObject(
        "timestamp_delivery" -> MessageTime()
      ))
    )
  }
}
