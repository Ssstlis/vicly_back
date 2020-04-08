package io.github.weakteam.model

import io.circe.Json
import io.circe.syntax._
import io.github.weakteam.model.UserChatSpec._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class UserChatSpec extends AnyWordSpec with Matchers {
  "UserChat#json" should {
    "valid decode" in {
      userChat.asJson mustBe userChatJson
    }

    "valid encode" in {
      userChatJson.as[UserChat] mustBe Right(userChat)
    }
  }
}

object UserChatSpec {
  val userChat = UserChat(1, 1, 1)

  val userChatJson = Json.obj(
    "id" -> 1.asJson,
    "userId" -> 1.asJson,
    "chatId" -> 1.asJson
  )
}
