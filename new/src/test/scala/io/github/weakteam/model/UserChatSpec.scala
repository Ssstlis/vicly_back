package io.github.weakteam.model

import eu.timepit.refined.types.numeric.PosLong
import io.circe.Json
import io.circe.syntax._
import io.github.weakteam.model.Chat.ChatId
import io.github.weakteam.model.User.UserId
import io.github.weakteam.model.UserChat.{RichUserChat, UserChatId}
import io.github.weakteam.model.UserChatSpec._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class UserChatSpec extends AnyWordSpec with Matchers {
  "UserChat#json" should {
    "valid decode" in {
      userChat.asJson mustBe userChatJson
    }

    "valid encode" in {
      userChatJson.as[RichUserChat] mustBe Right(userChat)
    }
  }
}

object UserChatSpec {
  val userChat: WithId[UserChatId, UserChat] = WithId(
    UserChatId(PosLong.unsafeFrom(1L)),
    UserChat(UserId(PosLong.unsafeFrom(1L)), ChatId(PosLong.unsafeFrom(1L)))
  )

  val userChatJson: Json = Json.obj(
    "id" -> 1.asJson,
    "userId" -> 1.asJson,
    "chatId" -> 1.asJson
  )
}
