package io.github.weakteam.model

import java.util.UUID

import eu.timepit.refined.types.numeric.PosLong
import io.circe.Json
import io.circe.syntax._
import io.github.weakteam.model.Group.GroupId
import io.github.weakteam.model.Invite.{InviteId, RichInvite}
import io.github.weakteam.model.Invite.InviteId._
import io.github.weakteam.model.WithId._
import io.github.weakteam.model.InviteSpec._
import io.github.weakteam.model.User.UserId
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class InviteSpec extends AnyWordSpec with Matchers {
  "Invite#json" should {
    "valid decode" in {
      invite.asJson mustBe inviteJson
    }

    "valid encode" in {
      inviteJson.as[RichInvite] mustBe Right(invite)
    }
  }
}

object InviteSpec {
  val uuid = UUID.randomUUID()

  val invite: WithId[InviteId, Invite] = WithId(
    InviteId(PosLong.unsafeFrom(1L)),
    Invite(
      firstName = "John",
      surname = Some("II"),
      lastName = "Doe",
      position = Some("employee"),
      uuid = uuid,
      groupId = GroupId(PosLong.unsafeFrom(1L)),
      inviterId = UserId(PosLong.unsafeFrom(1L))
    )
  )

  val inviteJson: Json = Json.obj(
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
