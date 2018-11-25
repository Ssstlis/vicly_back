package models

import org.bson.types.ObjectId
import play.api.libs.json.{Json, Writes}
import salat.annotations.Key

case class Role(
  _id: ObjectId,
  description: String,
  id: Int,
  @Key("group_id") groupId: Int
)

trait RoleJson {
  implicit val writes: Writes[Role] = (r: Role) => {
    Json.obj(
      "id" -> r.id,
      "description" -> r.description,
      "group_id" -> r.groupId
    )
  }
}

object Role extends RoleJson