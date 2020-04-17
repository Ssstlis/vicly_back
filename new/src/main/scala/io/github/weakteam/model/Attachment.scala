package io.github.weakteam.model

import java.time.OffsetDateTime

import cats.Show
import doobie.util.meta.Meta
import doobie.refined.implicits._
import eu.timepit.refined.types.numeric.PosLong
import io.circe.Codec
import io.circe.refined._
import io.estatico.newtype.macros.newtype
import io.github.weakteam.model.Message.MessageId
import io.github.weakteam.model.User.UserId
import cats.Show.catsContravariantForShow
import io.github.weakteam.util.refined.implicits._
import io.github.weakteam.util.circe.syntax._
import org.scalactic.anyvals.PosInt

final case class Attachment(
  fid: String,
  messageId: MessageId,
  userId: UserId,
  fileName: String,
  size: PosLong,
  isAvatar: Boolean = false,
  mime: String,
  date: OffsetDateTime,
  width: Option[PosInt],
  height: Option[PosInt]
)

object Attachment {

  @newtype
  final case class AttachmentId(id: PosLong)

  object AttachmentId {
    implicit val meta: Meta[AttachmentId]   = Meta[PosLong].imap(AttachmentId(_))(_.id)
    implicit val show: Show[AttachmentId]   = Show[PosLong].contramap(_.id)
    implicit val codec: Codec[AttachmentId] = deriveCodec[PosLong].imap(AttachmentId(_))(_.id)
  }

  type RichAttachment = WithId[AttachmentId, Attachment]
}
