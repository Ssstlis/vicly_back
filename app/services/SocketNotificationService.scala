package services

import com.google.inject.{Inject, Singleton}
import models.{Chat, Message}
import org.bson.types.ObjectId
import play.api.libs.json.{JsValue, Json}
import utils.JsonHelper.ObjectIdFormat

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

  def markRead(groupId: Int, id: ObjectId) = {
    push(5, groupId, Json.obj("id" -> id))
  }

  def markDelivery(groupId: Int, id: ObjectId) = {
    push(4, groupId, Json.obj("id" -> id))
  }

  def changed(groupId: Int, id: ObjectId) = {
    push(3, groupId, Json.obj("id" -> id))
  }

  def softDelete(groupId: Int, id: ObjectId) = {
    push(2, groupId, Json.obj("id" -> id))
  }

  def remove(groupId: Int, id: ObjectId) = {
    push(1, groupId, Json.obj("id" -> id))
  }

  def newMessage(groupId: Int, chatType: String, message: Message, chat: Chat) = {
    val json = Json.obj(
      "chat_type" -> chatType,
      "message" -> Json.toJson(message)(Message.writes(chat))
    )
    push(0, groupId, json)
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

  def userSetStatus(groupId: Int, userId: Int, status: String) = {
    val json = Json.obj(
      "user_id" -> userId,
      "status" -> status
    )
    push(30, groupId, json)
  }

  def userRemoveStatus(groupId: Int, userId: Int) = {
    push(31, groupId, Json.obj("user_id" -> userId))
  }

  def userSetAvatar(groupId: Int, userId: Int, uuid: String) = {
    val json = Json.obj(
    "user_id" -> userId,
    "uuid" -> uuid
  )
    push(32, groupId, json)
  }

  def userRemoveAvatar(groupId: Int, userId: Int) = {
    push(33, groupId, Json.obj("user_id" -> userId))
  }
}
