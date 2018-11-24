package models

import org.bson.types.ObjectId
import play.api.libs.json.{Json, Writes}
import salat.annotations.Key

case class Attachment(
  _id: ObjectId,
  uuid: String,
  @Key("user_id") userId: Int,
  filename: String,
  size: Long,
  timestamp: Int = (System.currentTimeMillis() / 1000).toInt
)

trait AttachmentJson {
  implicit val writes: Writes[Attachment] = (a: Attachment) => {
    Json.obj(
      "uuid" -> a.uuid,
      "user_id" -> a.userId,
      "filename" -> a.filename,
      "size" -> a.size,
      "timestamp" -> a.timestamp
    )
  }
}

object Attachment extends AttachmentJson