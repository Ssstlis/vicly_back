package io.github.weakteam.model

import java.time.OffsetDateTime

final case class Group(
  id: Int,
  name: String,
  ownerId: Int,
  date: OffsetDateTime,
  purpose: Option[String] = None
)
