package daos

import com.google.inject.{Inject, Singleton}
import com.mongodb.casbah.commons.MongoDBObject
import models.Group
import org.bson.types.ObjectId
import ru.tochkak.plugin.salat.PlaySalat
import salat.dao.{ModelCompanion, SalatDAO}
import utils.MongoDbHelper.MongoDbCursorExtended

@Singleton
class GroupDao @Inject()(
  mongoContext: MongoContext,
  playSalat: PlaySalat
) extends ModelCompanion[Group, ObjectId] {

  import mongoContext._

  val dao = new SalatDAO[Group, ObjectId](playSalat.collection("group", "ms")){}

  def all = {
    dao.find(MongoDBObject.empty).toList
  }

  def maxId = {
    dao.find(MongoDBObject.empty)
      .sort(MongoDBObject("id" -> -1))
      .foldHeadO(0)(_.id)
  }

  def create(group: Group) = save(group)

  def findById(id: Int) = {
    dao.findOne(MongoDBObject(
      "id" -> id
    ))
  }

  def setPurpose(id: Int, purpose: String) = {
    dao.update(
      MongoDBObject("id" -> id),
      MongoDBObject("$set" -> MongoDBObject(
        "purpose" -> purpose
      ))
    )
  }
}
