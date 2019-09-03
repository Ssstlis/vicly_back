package models.json

import models.{Attachment, Message}
import models.Attachment._
import play.api.libs.json.{JsObject, Json, Writes}
import utils.JsonHelper.ObjectIdFormat

object MessageJson {
  def writesWithAttachments: Writes[(Message, Seq[Attachment])] = {
    case (m: Message, attachments: Seq[Attachment]) =>
      Json.obj(
        "id" -> m._id,
        "from" -> m.from,
        "key" -> m.key,
        "message" -> m.text,
        "reply_for" -> m.replyForO,
        "timestamp_post" -> m.timestampPost,
        "timestamp_change" -> m.timestampChange,
        "timestamp_delivery" -> m.timestampDelivery,
        "timestamp_read" -> m.timestampRead,
        "attachments" -> attachments
      )
  }
}
