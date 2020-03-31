package io.github.weakteam.model

import io.circe.generic.JsonCodec

@JsonCodec
final case class Role(
    id: Int,
    groupId: Int,
    description: String
)
