package daos

import com.google.inject.{Inject, Singleton}
import com.mongodb.casbah.commons.MongoDBObject
import models.Chat
import org.bson.types.ObjectId
import ru.tochkak.plugin.salat.PlaySalat
import salat.dao.{ModelCompanion, SalatDAO}

@Singleton
class ChatDao @Inject()(
  mongoContext: MongoContext,
  playSalat: PlaySalat
) extends ModelCompanion[Chat, ObjectId] {

  import mongoContext._

  val dao = new SalatDAO[Chat, ObjectId](playSalat.collection("chat", "ms")){}

  def all = dao.find(MongoDBObject.empty).toList


  def findByIds(ids: List[Int]) = {
    dao.find(MongoDBObject(
      "id" -> MongoDBObject("$in" -> ids)
    )).toList
  }
}
