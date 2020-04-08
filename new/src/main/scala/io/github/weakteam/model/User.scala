package io.github.weakteam.model

import java.time.OffsetDateTime

final case class User(
  id: Int,
  firstName: String,
  lastName: String,
  surname: Option[String],
  position: Option[String],
  groupId: Option[Int],
  password: String,
  login: String,
  isActive: Boolean,
  joinDate: OffsetDateTime,
  lastActivity: OffsetDateTime,
  isArchive: Boolean = false,
  avatar: Option[Int],
  roleId: Int
)
