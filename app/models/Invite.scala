package models

import org.bson.types.ObjectId
import play.api.libs.json.{Json, Writes}
import salat.annotations.Key

case class Invite(
  @Key("first_name") firstName: String,
  @Key("last_name") lastName: String,
  uuid: String,
  @Key("group_id") groupId: Int,
  @Key("inviter_id") inviterId: Int,
  _id: ObjectId = new ObjectId()
)

trait InviteJson {
  implicit val writes: Writes[Invite] = (i: Invite) => {
    Json.obj(
      "first_name" -> i.firstName,
      "last_name" -> i.lastName,
      "uuid" -> i.uuid,
      "group_id" -> i.groupId,
      "inviter_id" -> i.inviterId
    )
  }
}

object Invite extends InviteJson