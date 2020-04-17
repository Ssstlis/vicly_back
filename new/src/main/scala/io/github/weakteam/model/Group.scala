package io.github.weakteam.model

import java.time.OffsetDateTime

import cats.Show
import doobie.refined.implicits._
import doobie.util.meta.Meta
import eu.timepit.refined.types.numeric.PosLong
import io.circe.Codec
import io.circe.generic.JsonCodec
import io.circe.refined._
import io.estatico.newtype.macros.newtype
import io.github.weakteam.model.User.UserId
import io.github.weakteam.util.circe.syntax._
import io.github.weakteam.util.refined.implicits._

@JsonCodec
final case class Group(
  name: String,
  ownerId: UserId,
  date: OffsetDateTime,
  purpose: Option[String] = None
)

object Group {

  @newtype
  final case class GroupId(id: PosLong)

  object GroupId {
    implicit val meta: Meta[GroupId]   = Meta[PosLong].imap(GroupId(_))(_.id)
    implicit val show: Show[GroupId]   = Show[PosLong].contramap(_.id)
    implicit val codec: Codec[GroupId] = deriveCodec[PosLong].imap(GroupId(_))(_.id)
  }

  type RichGroup = WithId[GroupId, Group]
}
