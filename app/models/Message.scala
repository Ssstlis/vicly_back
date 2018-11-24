package models

import org.bson.types.ObjectId
import play.api.libs.functional.syntax._
import play.api.libs.json._
import salat.annotations.Key
import utils.JsonHelper.ObjectIdFormat

case class Message(
  _id: ObjectId,
  from: Int,
  key: String,
  text: String,
  deleted: Boolean,
  @Key("reply_for") replyForO: Option[ObjectId],
  @Key("chat_id") chatId: Int,
  @Key("timestamp_post") timestampPost: MessageTime,
  @Key("timestamp_change") timestampChange: Option[MessageTime] = None,
  @Key("timestamp_delivery") timestampDelivery: Option[MessageTime] = None,
  @Key("timestamp_read") timestampRead: Option[MessageTime] = None
)

trait MessageJson {
  private def toJson(m: Message) = {
    Json.obj(
      "id" -> m._id,
      "from" -> m.from,
      "key" -> m.key,
      "message" -> m.text,
      "reply_for" -> m.replyForO,
      "timestamp_post" -> m.timestampPost,
      "timestamp_change" -> m.timestampChange,
      "timestamp_delivery" -> m.timestampDelivery,
      "timestamp_read" -> m.timestampRead
    )
  }

  implicit val writes: Writes[Message] = (m: Message) => toJson(m)

  def writes(chatO: Option[Chat]): Writes[Message] = (m: Message) => {
    toJson(m) + ("chat" -> Json.toJson(chatO))
  }

  def reads(from: Int): Reads[Message] = (
    Reads.pure(new ObjectId()) and
    Reads.pure(from) and
    ((__ \ "key").read[String] orElse Reads.pure("")) and
    (__ \ "message").read[String] and
    Reads.pure(false) and
    (__ \ "reply_for").readNullable[ObjectId] and
    (__ \ "id").read[Int] and
    Reads.pure(MessageTime()) and
    Reads.pure(None) and
    Reads.pure(None) and
    Reads.pure(None)
  )(Message.apply _)
}

object Message extends MessageJson