package models

import org.bson.types.ObjectId
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._
import salat.annotations.Key
import utils.Helper.DateTimeExtended
import utils.JsonHelper.ObjectIdFormat

case class User(
  _id: ObjectId,
  @Key("first_name") firstName: String,
  @Key("last_name") lastName: String,
  @Key("group_id") groupId: Option[Int],
  password: String,
  login: String,
  active: Boolean,
  @Key("join_time") joinTime: Int,
  id: Int
)

trait UserJson {
  private def toJson(u: User) = {
    Json.obj(
      "_id" -> u._id,
      "id" -> u.id,
      "first_name" -> u.firstName,
      "last_name" -> u.lastName,
      "group_id" -> u.groupId,
      "login" -> u.login,
      "is_active" -> u.active,
      "join_time" -> u.joinTime,
    )
  }

  implicit val writes: Writes[User] = (u: User) => {
    toJson(u)
  }

  def writesWithToken(token: String): Writes[User] = (u: User) => {
    toJson(u) + ("token" -> Json.toJson(token))
  }

  def reads(id: => Int): Reads[User] = (
    Reads.pure(new ObjectId()) and
    ((__ \ "first_name").read[String] orElse Reads.pure("")) and
    ((__ \ "last_name").read[String] orElse Reads.pure("")) and
    (__ \ "group_id").readNullable[Int] and
    (__ \ "password").read[String] and
    (__ \ "login").read[String] and
    Reads.pure(true) and
    Reads.pure(DateTime.now.timestamp) and
    Reads.pure(id)
  )(User.apply _)
}

object User extends UserJson