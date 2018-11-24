package services

import com.google.inject.{Inject, Singleton}
import models.{Chat, Message}
import play.api.libs.json.{JsValue, Json}

@Singleton
class SocketNotificationService @Inject()(subscriberService: SubscriberService) {

  private def push(event: Int, groupId: Int, json: JsValue) = {
    subscriberService.subscriptionSubscribers(groupId).foreach(subscriber =>
      subscriber ! Json.obj(
        "event" -> event,
        "message" -> json
      )
    )
  }

  def markRead(groupId: Int, jsValue: JsValue) = push(5, groupId, jsValue)

  def markDelivery(groupId: Int, jsValue: JsValue) = push(4, groupId, jsValue)

  def changed(groupId: Int, jsValue: JsValue) = push(3, groupId, jsValue)

  def softDelete(groupId: Int, jsValue: JsValue) = push(2, groupId, jsValue)

  def remove(groupId: Int, jsValue: JsValue) = push(1, groupId, jsValue)

  def newMessage(groupId: Int, chatType: String, message: Message, chat: Chat) = {
    val json = Json.obj(
      "chat_type" -> chatType,
      "message" -> Json.toJson(message)(Message.writes(chat))
    )
    push(0, groupId, json)
  }

  def activity(groupIdO: Option[Int], userId: Int) = {
    push(10, groupIdO.getOrElse(-1), Json.obj("id" -> userId))
  }

  def online(groupIdO: Option[Int], userId: Int) = {
    push(11, groupIdO.getOrElse(-1), Json.obj("id" -> userId))
  }

  def offline(groupIdO: Option[Int], userId: Int) = {
    push(12, groupIdO.getOrElse(-1), Json.obj("id" -> userId))
  }

  def archive(groupIdO: Option[Int], userId: Int) = {
    push(13, groupIdO.getOrElse(-1), Json.obj("id" -> userId))
  }

  def typing(groupId: Int, userId: Int, chatId: Int, chatType: String) = {
    val json = Json.obj(
      "user_id" -> userId,
      "chat_id" -> chatId,
      "chat_type" -> chatType
    )
    push(20, groupId, json)
  }

  def newGroupChat(groupId: Int, userIds: List[Int]) = {
    push(21, groupId, Json.toJson(userIds))
  }

  def newUserInChat(groupId: Int, chatId: Int, userId: Int) = {
    val json = Json.obj(
      "chat_id" -> chatId,
      "user_id" -> userId
    )
    push(22, groupId, json)
  }

  def removeUserInChat(groupId: Int, chatId: Int, userId: Int) = {
    val json = Json.obj(
      "chat_id" -> chatId,
      "user_id" -> userId
    )
    push(23, groupId, json)
  }

  def archiveChat(groupId: Int, chatId: Int) = {
    push(24, groupId, Json.obj("chat_id" -> chatId))
  }
}
