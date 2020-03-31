package io.github.weakteam.model

import io.circe.Json
import io.circe.syntax._
import io.github.weakteam.model.RoleSpec._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class RoleSpec extends AnyWordSpec with Matchers {

  "Role#json" should {
    "valid decode" in {
      role.asJson mustBe roleJson
    }

    "valid encode" in {
      roleJson.as[Role].map(_ mustBe role)
    }
  }
}

object RoleSpec {
  val role = Role(1, 1, "test description")

  val roleJson = Json.obj(
    "id" -> Json.fromInt(1),
    "groupId" -> Json.fromInt(1),
    "description" -> Json.fromString("test description")
  )
}
