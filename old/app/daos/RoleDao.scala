package daos

import com.google.inject.{Inject, Singleton}
import com.mongodb.casbah.commons.MongoDBObject
import models.Role
import org.bson.types.ObjectId
import ru.tochkak.plugin.salat.PlaySalat
import salat.dao.{ModelCompanion, SalatDAO}
import utils.MongoDbHelper.MongoDbCursorExtended

@Singleton
class RoleDao @Inject()(
  mongoContext: MongoContext,
  playSalat: PlaySalat
) extends ModelCompanion[Role, ObjectId] {

  import mongoContext._

  val dao = new SalatDAO[Role, ObjectId](playSalat.collection("role", "ms")){}

  def maxId = {
    dao.find(MongoDBObject.empty)
      .sort(MongoDBObject("id" -> -1))
      .foldHeadO(0)(_.id)
  }

  def nextId = maxId + 1

  def find(id: Int, groupId: Int) = {
    dao.findOne(MongoDBObject(
      "id" -> id,
      "group_id" -> groupId
    ))
  }

  def find(id: Int) = {
    dao.findOne(MongoDBObject("id" -> id))
  }

  def findGroupRoles(groupId: Int) = {
    dao.find(MongoDBObject("group_id" -> groupId)).toList
  }

  def save(description: String, groupId: Int) = {
    dao.insert(Role(new ObjectId(), description, nextId, groupId )).isDefined
  }

  def update(id: Int, description: String, groupId: Int) = {
    dao.update(
      MongoDBObject(
        "id" -> id,
        "group_id" -> groupId
      ),
      MongoDBObject(
        "$set" -> MongoDBObject(
          "description" -> description
        )
      )
    )
  }

  def remove(id: Int) = {
    dao.remove(MongoDBObject("id" -> id))
  }
}
