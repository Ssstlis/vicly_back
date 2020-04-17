package io.github.weakteam.model

import java.util.UUID

import cats.Show
import cats.Show.catsContravariantForShow
import doobie.util.meta.Meta
import doobie.refined.implicits._
import eu.timepit.refined.types.numeric.PosLong
import io.circe.Codec
import io.circe.generic.JsonCodec
import io.circe.refined._
import io.estatico.newtype.macros.newtype
import io.github.weakteam.model.Group.GroupId
import io.github.weakteam.model.User.UserId
import io.github.weakteam.util.circe.syntax._
import io.github.weakteam.util.refined.implicits._

@JsonCodec
final case class Invite(
  firstName: String,
  surname: Option[String],
  lastName: String,
  position: Option[String],
  uuid: UUID,
  groupId: GroupId,
  inviterId: UserId
)

object Invite {

  @newtype
  final case class InviteId(id: PosLong)

  object InviteId {
    implicit val meta: Meta[InviteId]   = Meta[PosLong].imap(InviteId(_))(_.id)
    implicit val show: Show[InviteId]   = Show[PosLong].contramap(_.id)
    implicit val codec: Codec[InviteId] = deriveCodec[PosLong].imap(InviteId(_))(_.id)
  }

  type RichInvite = WithId[InviteId, Invite]
}
