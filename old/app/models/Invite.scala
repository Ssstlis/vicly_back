package models

import org.bson.types.ObjectId
import play.api.libs.json.{Json, Writes}
import salat.annotations.Key

case class Invite(
  @Key("first_name") firstName: String,
  surname: Option[String],
  @Key("last_name") lastName: String,
  position: Option[String],
  uuid: String,
  @Key("group_id") groupId: Int,
  @Key("inviter_id") inviterId: Int,
  _id: ObjectId = new ObjectId()
) {
  def toJson = {
    Json.obj(
      "first_name" -> firstName,
      "surname" -> surname,
      "last_name" -> lastName,
      "position" -> position,
      "uuid" -> uuid,
      "group_id" -> groupId,
      "inviter_id" -> inviterId
    )
  }
}

trait InviteJson {
  implicit val writes: Writes[Invite] = (i: Invite) => i.toJson

  def writesWithGroupName(groupNameO: Option[String]): Writes[Invite] = (i: Invite) => {
    i.toJson + ("group_name" -> Json.toJson(groupNameO))
  }
}

object Invite extends InviteJson