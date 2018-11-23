package models

import play.api.libs.json.{Json, Writes}
import salat.annotations.Key

case class Chat(
  id: Int,
  @Key("group_id") groupId: Int,
  @Key("user_ids") userIds: List[Int],
  @Key("chat_type") chatType: String
)

trait ChatJson {
  implicit val writes: Writes[Chat] = (c: Chat) => {
    Json.obj(
      "id" -> c.id,
      "group_id" -> c.groupId,
      "user_ids" -> c.userIds
    )
  }
}

object Chat extends ChatJson