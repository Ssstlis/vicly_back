package models

import org.bson.types.ObjectId
import org.joda.time.DateTime
import play.api.libs.json.{Json, Writes}
import utils.Helper.DateTimeExtended
import utils.JsonHelper.ObjectIdFormat

case class Group(
  _id: ObjectId,
  id: Int,
  name: String,
  created: Int,
  owner: ObjectId,
  purpose: Option[String] = None
)

trait GroupJson {
  implicit val writes: Writes[Group] = (g: Group) => {
    Json.obj(
      "id" -> g.id,
      "name" -> g.name,
      "created" -> g.created,
      "owner" -> g.owner,
      "purpose" -> g.purpose
    )
  }
}

object Group extends GroupJson {
  def apply(user: User, name: String, id: Int): Group = {
    new Group(new ObjectId(), id, name, DateTime.now.timestamp, user._id)
  }
}