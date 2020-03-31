package io.github.weakteam.model

final case class Chat(
    id: Int,
    groupId: Int,
    chatTypeId: Int,
    ownerId: Int,
    isArchive: Boolean,
    name: String,
    description: String,
    isPrivate: Boolean
)
