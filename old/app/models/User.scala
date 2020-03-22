package models

import org.bson.types.ObjectId
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._
import salat.annotations.Key
import utils.Helper.DateTimeExtended
import utils.JsonHelper.ObjectIdFormat

import scala.annotation.meta.field

case class User(
                 _id: ObjectId,
                 @Key("first_name") firstName: String,
                 surname: Option[String],
                 @Key("last_name") lastName: String,
                 position: Option[String],
                 @Key("group_id") groupId: Option[Int],
                 password: String,
                 login: String,
                 active: Boolean,
                 @Key("join_time") joinTime: Int,
                 id: Int,
                 @Key("last_activity") lastActivity: Int = 0,
                 archive: Boolean = false,
                 avatar: Option[ObjectId],
                 @Key("role_id") roleId: Int = 0
               ) {
  def toJson = {
    Json.obj(
      "_id" -> _id,
      "id" -> id,
      "first_name" -> firstName,
      "surname" -> surname,
      "last_name" -> lastName,
      "position" -> position,
      "group_id" -> groupId,
      "login" -> login,
      "is_active" -> active,
      "join_time" -> joinTime,
      "last_activity" -> lastActivity,
      "archive" -> archive,
      "avatar" -> avatar,
      "role_id" -> roleId
    )
  }

  val claim = Json.obj(
    "user_id" -> _id,
    "login" -> login,
    "password" -> password
  )
}

trait UserJson {

  implicit val writes: Writes[User] = (u: User) => {
    u.toJson
  }

  def writesWithToken(token: String): Writes[User] = (u: User) => {
    u.toJson + ("token" -> Json.toJson(token))
  }

  def reads(id: => Int): Reads[User] = (
    Reads.pure(new ObjectId()) and
      ((__ \ "first_name").read[String] orElse Reads.pure("")) and
      (__ \ "surname").readNullable[String] and
      ((__ \ "last_name").read[String] orElse Reads.pure("")) and
      (__ \ "surname").readNullable[String] and
      (__ \ "group_id").readNullable[Int] and
      (__ \ "password").read[String] and
      (__ \ "login").read[String] and
      Reads.pure(true) and
      Reads.pure(DateTime.now.timestamp) and
      Reads.pure(id) and
      Reads.pure((System.currentTimeMillis() / 1000).toInt) and
      Reads.pure(false) and
      Reads.pure(None) and
      ((__ \ "role_id").read[Int] orElse Reads.pure(0))
    ) (User.apply _)
}

object User extends UserJson