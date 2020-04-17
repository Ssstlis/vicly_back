package io.github.weakteam.model

import java.time.OffsetDateTime

import cats.Show
import doobie.util.meta.Meta
import doobie.refined.implicits._
import eu.timepit.refined.types.numeric.PosLong
import io.circe.Codec
import io.circe.generic.JsonCodec
import io.circe.refined._
import io.estatico.newtype.macros.newtype
import io.github.weakteam.model.Chat.ChatId
import io.github.weakteam.model.User.UserId
import io.github.weakteam.util.circe.syntax._
import io.github.weakteam.util.refined.implicits._

@JsonCodec
final case class Message(
  from: UserId,
  key: String,
  text: String,
  isDeleted: Boolean,
  threadId: Option[PosLong],
  quoteFor: Option[PosLong],
  chatId: ChatId,
  datePost: OffsetDateTime,
  dateChange: Option[OffsetDateTime] = None,
  dateDelivery: Option[OffsetDateTime] = None,
  dateRead: Option[OffsetDateTime] = None
)

object Message {

  @newtype
  final case class MessageId(id: PosLong)

  object MessageId {
    implicit val meta: Meta[MessageId]   = Meta[PosLong].imap(MessageId(_))(_.id)
    implicit val show: Show[MessageId]   = Show[PosLong].contramap(_.id)
    implicit val codec: Codec[MessageId] = deriveCodec[PosLong].imap(MessageId(_))(_.id)
  }

  type RichMessage = WithId[MessageId, Message]
}
