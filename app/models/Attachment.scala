package models

import org.bson.types.ObjectId
import play.api.libs.json.{Json, Writes}
import salat.annotations.Key
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import utils.JsonHelper.ObjectIdFormat

case class Attachment(
  _id: ObjectId,
  fid: String,
  @Key("user_id") userId: Int,
  filename: String,
  size: Long,
  @Key("is_avatar") isAvatar: Boolean,
  mime: String,
  metadata: Map[String, String] = Map("fulfilled" -> "false"),
  timestamp: Int = (System.currentTimeMillis() / 1000).toInt,
  previewSmall: Option[Attachment] = None,
  previewBig: Option[Attachment] = None,
)

trait AttachmentJson {
//  implicit val writes: Writes[Attachment] = (a: Attachment) => {
//    Json.obj(
//      "id" -> a._id,
//      "user_id" -> a.userId,
//      "filename" -> a.filename,
//      "size" -> a.size,
//      "timestamp" -> a.timestamp,
//      "is_avatar" -> a.isAvatar,
//      "mime" -> a.mime,
//      "metadata" -> a.metadata,
//      "previewSmall" -> writes(a.previewSmall),
//      "previewBig" -> writes(a.previewBig)
//    )
//  }

    implicit lazy val writes:Writes[Attachment] = (
      (__ \ "id").write[ObjectId] and
      (__ \ "fid").write[String] and
      (__ \ "user_id").write[Int] and
      (__ \ "filename").write[String] and
      (__ \ "size").write[Long] and
      (__ \ "is_avatar").write[Boolean] and
      (__ \ "mime").write[String] and
      (__ \ "metadata").write[Map[String, String]] and
      (__ \ "timestamp").write[Int] and
      (__ \ "previewSmall").lazyWriteNullable(writes) and
      (__ \ "previewBig").lazyWriteNullable(writes)
      )(unlift(Attachment.unapply))
}

object Attachment extends AttachmentJson