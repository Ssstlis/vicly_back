package io.github.weakteam.model

import java.time.OffsetDateTime

final case class Attachment(
    id: Int,
    fid: String,
    messageId: Int,
    userId: Int,
    fileName: String,
    size: Long,
    isAvatar: Boolean = false,
    mime: String,
    date: OffsetDateTime,
    width: Option[Int],
    height: Option[Int]
)
