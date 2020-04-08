package io.github.weakteam.model

import java.time.OffsetDateTime

final case class Message(
  id: Int,
  from: Int,
  key: String,
  text: String,
  isDeleted: Boolean,
  threadId: Option[Int],
  quoteFor: Option[Int],
  chatId: Int,
  datePost: OffsetDateTime,
  dateChange: Option[OffsetDateTime] = None,
  dateDelivery: Option[OffsetDateTime] = None,
  dateRead: Option[OffsetDateTime] = None
)
