package io.github.weakteam.model

import java.util.UUID

import io.circe.generic.JsonCodec

@JsonCodec
final case class Invite(
  id: Int,
  firstName: String,
  surname: Option[String],
  lastName: String,
  position: Option[String],
  uuid: UUID,
  groupId: Int,
  inviterId: Int
)
