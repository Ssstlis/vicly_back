package io.github.weakteam.model

import java.util.UUID

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
