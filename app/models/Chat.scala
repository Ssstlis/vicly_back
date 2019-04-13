package models

import org.bson.types.ObjectId
import play.api.libs.json.{Json, Reads, Writes, __}
import salat.annotations.Key
import ru.tochkak.plugin.salat.Binders.objectIdWrites

case class Chat(
  id: Int,
  @Key("group_id") groupId: Option[Int] = None,
  @Key("user_ids") userIds: List[Int],
  @Key("chat_type") chatType: String,
  archive: Option[Boolean] = Some(false),
  owner: Option[ObjectId] = None,
  name: Option[String] = None,
  purpose: Option[String] = None,
  @Key("private") isPrivate: Boolean = false
)

trait ChatJson {
  implicit val writes: Writes[Chat] = (c: Chat) => {
    val json = Json.obj(
      "id" -> c.id,
      "user_ids" -> c.userIds,
      "chat_type" -> c.chatType
    )
    c.chatType match {
      case "user" => json
      case "group" => json ++ Json.obj(
        "group_id" -> c.groupId,
        "archive" -> c.archive,
        "owner" -> c.owner,
        "name" -> c.name,
        "purpose" -> c.purpose,
        "private" -> c.isPrivate
      )
    }
  }
}

object Chat extends ChatJson