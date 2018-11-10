package models

import org.bson.types.ObjectId
import play.api.libs.json.{Json, Writes}
import salat.annotations.raw.Key
import utils.JsonHelper.ObjectIdFormat

case class User(
  _id: ObjectId,
  @Key("first_name") firstName: String,
  @Key("last_name") lastName: String,
  @Key("group_id") groupId: Int,
  password: String,
  login: String,
  active: Boolean,
  @Key("join_time") joinTime: Long
)

trait UserJson {
  implicit val writes: Writes[User] = (u: User) => {
    Json.obj(
      "id" -> u._id,
      "first_name" -> u.firstName,
      "last_name" -> u.lastName,
      "group_id" -> u.groupId,
      "login" -> u.login,
      "is_active" -> u.active,
      "join_time" -> u.joinTime
    )
  }
}

object User extends UserJson