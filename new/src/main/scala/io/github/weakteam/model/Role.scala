package io.github.weakteam.model

import cats.Show
import cats.syntax.show._
import cats.instances.string._
import cats.instances.option._
import doobie.util.meta.Meta
import doobie.refined.implicits._
import eu.timepit.refined.types.numeric.PosLong
import io.circe.Codec
import io.circe.generic.JsonCodec
import io.circe.refined._
import io.estatico.newtype.macros.newtype
import io.github.weakteam.model.Group.GroupId
import io.github.weakteam.util.refined.implicits._
import io.github.weakteam.util.circe.syntax._
import tofu.logging.Loggable

@JsonCodec
final case class Role(
  groupId: GroupId,
  description: Option[String]
)

object Role {

  @newtype //(debug = true)
  final case class RoleId(id: PosLong)

  object RoleId {
    implicit val meta: Meta[RoleId]   = Meta[PosLong].imap(RoleId(_))(_.id)
    implicit val show: Show[RoleId]   = Show[PosLong].contramap(_.id)
    implicit val codec: Codec[RoleId] = deriveCodec[PosLong].imap(RoleId(_))(_.id)
  }

  type RichRole = WithId[RoleId, Role]

  implicit val roleShow: Show[Role] = role =>
    show"Role(groupId = ${role.groupId.id}, description = ${role.description})"

  implicit val roleLoggable: Loggable[Role] = Loggable.show[Role]
}
