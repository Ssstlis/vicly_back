package io.github.weakteam.model

import eu.timepit.refined.types.numeric.PosInt
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
      roleJson.as[Role] mustBe Right(role)
    }
  }
}

object RoleSpec {
  val role = Role(PosInt.unsafeFrom(1), PosInt.unsafeFrom(1), Some("test description"))

  val roleJson = Json.obj(
    "id" -> 1.asJson,
    "groupId" -> 1.asJson,
    "description" -> "test description".asJson
  )
}
