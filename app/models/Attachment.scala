package models

import org.bson.types.ObjectId
import play.api.libs.json.{Json, Writes}
import salat.annotations.Key
import utils.JsonHelper.ObjectIdFormat

case class Attachment(
  _id: ObjectId,
  fid: String,
  @Key("user_id") userId: Int,
  filename: String,
  size: Long,
  @Key("is_avatar") isAvatar: Boolean,
  metadata: Map[String, String] = Map("fulfilled" -> "false"),
  timestamp: Int = (System.currentTimeMillis() / 1000).toInt
)

trait AttachmentJson {
  implicit val writes: Writes[Attachment] = (a: Attachment) => {
    Json.obj(
      "id" -> a._id,
      "user_id" -> a.userId,
      "filename" -> a.filename,
      "size" -> a.size,
      "timestamp" -> a.timestamp,
      "is_avatar" -> a.isAvatar,
      "metadata" -> a.metadata
    )
  }
}

object Attachment extends AttachmentJson