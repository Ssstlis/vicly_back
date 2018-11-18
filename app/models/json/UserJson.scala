package models.json

import models.{Group, User}
import play.api.libs.json.Json

object UserJson {

  def writesUsersPerGroups(withoutGroup: Seq[User], withGroups: Map[Group, Seq[User]]) = {
    Json.obj(
      "with_group" -> withGroups.map { case (group, users) => Json.obj(
        "group" -> group,
        "users" -> users
      )},
      "without_group" -> withoutGroup
    )
  }
}