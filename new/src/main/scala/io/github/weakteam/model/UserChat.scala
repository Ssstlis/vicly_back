package io.github.weakteam.model

import io.circe.generic.JsonCodec

@JsonCodec
final case class UserChat(
  id: Int,
  userId: Int,
  chatId: Int
)
