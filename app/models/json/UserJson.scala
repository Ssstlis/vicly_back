package models.json

import models.{Group, User}
import play.api.libs.json.Json

object UserJson {

  def writesUsersPerGroups(withoutGroup: List[User], withGroups: Map[Group, List[User]]) = {
    Json.obj(
      "with_group" -> withGroups.map { case (group, users) => Json.obj(
        "group" -> group,
        "users" -> users
      )},
      "without_group" -> withoutGroup
    )
  }
}