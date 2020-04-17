package io.github.weakteam.model

import cats.Show
import doobie.util.meta.Meta
import doobie.refined.implicits._
import eu.timepit.refined.types.numeric.PosLong
import io.circe.Codec
import io.circe.generic.JsonCodec
import io.circe.refined._
import io.estatico.newtype.macros.newtype
import io.github.weakteam.util.circe.syntax._
import io.github.weakteam.util.refined.implicits._

@JsonCodec
final case class ChatType(
  name: String
)

object ChatType {

  @newtype
  final case class ChatTypeId(id: PosLong)

  object ChatTypeId {
    implicit val meta: Meta[ChatTypeId]   = Meta[PosLong].imap(ChatTypeId(_))(_.id)
    implicit val show: Show[ChatTypeId]   = Show[PosLong].contramap(_.id)
    implicit val codec: Codec[ChatTypeId] = deriveCodec[PosLong].imap(ChatTypeId(_))(_.id)
  }

  type RichChatType = WithId[ChatTypeId, ChatType]
}
