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
import io.github.weakteam.model.Group.GroupId
import io.github.weakteam.model.Role.RoleId
import io.github.weakteam.util.circe.syntax._
import io.github.weakteam.util.refined.implicits._

@JsonCodec
final case class User(
  firstName: String,
  lastName: String,
  surname: Option[String],
  position: Option[String],
  groupId: Option[GroupId],
  password: String,
  login: String,
  isActive: Boolean,
  joinDate: OffsetDateTime,
  lastActivity: OffsetDateTime,
  isArchive: Boolean = false,
  avatar: Option[Int],
  roleId: RoleId
)

object User {

  @newtype
  final case class UserId(id: PosLong)

  object UserId {
    implicit val meta: Meta[UserId]   = Meta[PosLong].imap(UserId(_))(_.id)
    implicit val show: Show[UserId]   = Show[PosLong].contramap(_.id)
    implicit val codec: Codec[UserId] = deriveCodec[PosLong].imap(UserId(_))(_.id)
  }

  type RichUser = WithId[UserId, User]
}
