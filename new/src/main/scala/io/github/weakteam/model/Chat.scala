package io.github.weakteam.model

import cats.Show
import doobie.util.meta.Meta
import doobie.refined.implicits._
import eu.timepit.refined.types.numeric.PosLong
import io.circe.Codec
import io.circe.generic.JsonCodec
import io.circe.refined._
import io.estatico.newtype.macros.newtype
import io.github.weakteam.model.ChatType.ChatTypeId
import io.github.weakteam.model.Group.GroupId
import io.github.weakteam.model.User.UserId
import io.github.weakteam.util.circe.syntax._
import io.github.weakteam.util.refined.implicits._

@JsonCodec
final case class Chat(
  groupId: GroupId,
  chatTypeId: ChatTypeId,
  ownerId: UserId,
  isArchive: Boolean,
  name: String,
  description: String,
  isPrivate: Boolean
)

object Chat {

  @newtype
  final case class ChatId(id: PosLong)

  object ChatId {
    implicit val meta: Meta[ChatId]   = Meta[PosLong].imap(ChatId(_))(_.id)
    implicit val show: Show[ChatId]   = Show[PosLong].contramap(_.id)
    implicit val codec: Codec[ChatId] = deriveCodec[PosLong].imap(ChatId(_))(_.id)
  }

  type RichChat = WithId[ChatId, Chat]
}
