package models

import org.bson.types.ObjectId
import play.api.libs.json.{Json, Writes}
import salat.annotations.Key
import ru.tochkak.plugin.salat.Binders.objectIdWrites

case class Chat(
  id: Int,
  @Key("group_id") groupId: Int,
  @Key("user_ids") userIds: List[Int],
  @Key("chat_type") chatType: String,
  @Key("archive") archive: Option[Boolean],
  owner: Option[ObjectId]
)

trait ChatJson {
  implicit val writes: Writes[Chat] = (c: Chat) => {
    val json = Json.obj(
      "id" -> c.id,
      "group_id" -> c.groupId,
      "user_ids" -> c.userIds,
      "chat_type" -> c.chatType
    )

    c.chatType match {
      case "user" => json
      case "group" => json + ("archive" -> Json.toJson(c.archive)) + ("owner" -> Json.toJson(c.owner))
    }
  }
}

object Chat extends ChatJson