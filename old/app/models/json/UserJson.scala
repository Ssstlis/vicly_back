package models.json

import models.{Chat, Group, Message, User}
import play.api.libs.json.Json

object UserJson {

  def writesUsersPerGroups(
    withoutGroup: Seq[User],
    withGroups: Map[Group, (Seq[(User, Long, Option[Message], Option[Chat])], Map[Int, (Long, Option[Message], Chat)])]
  ) = {
    Json.obj(
      "with_group" -> withGroups.map { case (group, (usersWithMessages, groupChatMap)) =>
        Json.obj(
          "group" -> group,
          "group_chats" -> groupChatMap.map { case (chatId, (unread, lastO, chat)) =>
            Json.obj(
              "chat_id" -> chatId,
              "unread" -> unread,
              "last" -> lastO,
              "chat" -> chat
            )
          },
          "users" -> usersWithMessages.map { case (user, unread, lastO, chat) =>
            Json.obj(
              "user" -> user,
              "user_chat" -> chat,
              "unread" -> unread,
              "last" -> lastO
            )
          }
        )
      },
      "without_group" -> withoutGroup
    )
  }
}