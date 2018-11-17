package models

import org.bson.types.ObjectId
import play.api.libs.json.{Json, Writes}

case class Chat(id: Int, groupId: Int, users: List[ObjectId])

trait ChatJson {
  implicit val writes: Writes[Chat] = (c: Chat) => {
    Json.obj(
      "id" -> c.id,
      "group_id" -> c.groupId
    )
  }
}

object Chat extends ChatJson