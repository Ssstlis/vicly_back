package io.github.weakteam.model

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
final case class UserChat(
  userId: UserId,
  chatId: ChatId
)

object UserChat {

  @newtype
  final case class UserChatId(id: PosLong)

  object UserChatId {
    implicit val meta: Meta[UserChatId]   = Meta[PosLong].imap(UserChatId(_))(_.id)
    implicit val show: Show[UserChatId]   = Show[PosLong].contramap(_.id)
    implicit val codec: Codec[UserChatId] = deriveCodec[PosLong].imap(UserChatId(_))(_.id)
  }

  type RichUserChat = WithId[UserChatId, UserChat]
}
