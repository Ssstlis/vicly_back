package io.github.weakteam.model

import eu.timepit.refined.types.numeric.PosLong
import io.circe.Json
import io.circe.syntax._
import io.github.weakteam.model.Group.GroupId
import io.github.weakteam.model.Role.{RichRole, RoleId}
import io.github.weakteam.model.RoleSpec._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class RoleSpec extends AnyWordSpec with Matchers {

  "Role#json" should {
    "valid decode" in {
      role.asJson mustBe roleJson
    }

    "valid encode" in {
      roleJson.as[RichRole] mustBe Right(role)
    }
  }
}

object RoleSpec {
  val role: WithId[RoleId, Role] = WithId(
    RoleId(PosLong.unsafeFrom(1L)),
    Role(GroupId(PosLong.unsafeFrom(1L)), Some("test description"))
  )

  val roleJson: Json = Json.obj(
    "id" -> 1.asJson,
    "groupId" -> 1.asJson,
    "description" -> "test description".asJson
  )
}
