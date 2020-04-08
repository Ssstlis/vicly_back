package io.github.weakteam.model

import java.util.UUID

import io.circe.Json
import io.circe.syntax._
import io.github.weakteam.model.InviteSpec._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class InviteSpec extends AnyWordSpec with Matchers {
  "Invite#json" should {
    "valid decode" in {
      invite.asJson mustBe inviteJson
    }

    "valid encode" in {
      inviteJson.as[Invite] mustBe Right(invite)
    }
  }
}

object InviteSpec {
  val uuid = UUID.randomUUID()

  val invite = Invite(
    id = 1,
    firstName = "John",
    surname = Some("II"),
    lastName = "Doe",
    position = Some("employee"),
    uuid = uuid,
    groupId = 1,
    inviterId = 1
  )

  val inviteJson = Json.obj(
    "id" -> 1.asJson,
    "firstName" -> "John".asJson,
    "surname" -> "II".asJson,
    "lastName" -> "Doe".asJson,
    "position" -> "employee".asJson,
    "uuid" -> uuid.asJson,
    "groupId" -> 1.asJson,
    "inviterId" -> 1.asJson
  )
}
