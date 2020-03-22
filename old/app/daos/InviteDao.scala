package daos

import com.google.inject.{Inject, Singleton}
import com.mongodb.casbah.commons.MongoDBObject
import models.Invite
import org.bson.types.ObjectId
import ru.tochkak.plugin.salat.PlaySalat
import salat.dao.{ModelCompanion, SalatDAO}

@Singleton
class InviteDao @Inject()(
  mongoContext: MongoContext,
  playSalat: PlaySalat
) extends ModelCompanion[Invite, ObjectId] {

  import mongoContext._

  val dao = new SalatDAO[Invite, ObjectId](playSalat.collection("invite", "ms")) {}

  def find(uuid: String) = {
    dao.findOne(MongoDBObject(
      "uuid" -> uuid
    ))
  }

  def all = {
    dao.find(MongoDBObject.empty).toList
  }
}