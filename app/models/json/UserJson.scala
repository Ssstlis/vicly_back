package models.json

import models.{Group, Message, User}
import play.api.libs.json.Json

object UserJson {

  def writesUsersPerGroups(
    withoutGroup: Seq[User],
    withGroups: Map[Group, Seq[(User, Long, Option[Message])]]
  ) = {
    Json.obj(
      "with_group" -> withGroups.map { case (group, usersWithMessages) =>
        Json.obj(
          "group" -> group,
          "users" -> usersWithMessages.map { case (user, messages, lastMessage) =>
            Json.obj(
              "user" -> user,
              "undread" -> messages,
              "last" -> lastMessage
            )
          }
        )
      },
      "without_group" -> withoutGroup
    )
  }
}