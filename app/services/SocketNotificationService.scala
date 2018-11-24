package services

import com.google.inject.{Inject, Singleton}
import models.{Chat, Message}
import org.bson.types.ObjectId
import play.api.libs.json.{JsValue, Json}
import utils.JsonHelper.ObjectIdFormat

@Singleton
class SocketNotificationService @Inject()(subscriberService: SubscriberService) {

  private def push(event: Int, groupId: Int, json: JsValue, filter: Int => Boolean = (_: Int) => true) = {
    subscriberService.subscriptionSubscribers(groupId).foreach {
      case (subscriber, userId) if filter(userId) =>
      subscriber ! Json.obj(
        "event" -> event,
        "message" -> json
      )
    }
  }

  def markRead(groupId: Int, id: ObjectId, userIds: List[Int]) = {
    push(5, groupId, Json.obj("id" -> id), userIds.contains)
  }

  def markDelivery(groupId: Int, id: ObjectId, userIds: List[Int]) = {
    push(4, groupId, Json.obj("id" -> id), userIds.contains)
  }

  def changed(groupId: Int, id: ObjectId, userIds: List[Int]) = {
    push(3, groupId, Json.obj("id" -> id), userIds.contains)
  }

  def softDelete(groupId: Int, id: ObjectId, userIds: List[Int]) = {
    push(2, groupId, Json.obj("id" -> id), userIds.contains)
  }

  def remove(groupId: Int, id: ObjectId, userIds: List[Int]) = {
    push(1, groupId, Json.obj("id" -> id), userIds.contains)
  }

  def newMessage(groupId: Int, chatType: String, message: Message, chat: Chat) = {
    val json = Json.obj(
      "chat_type" -> chatType,
      "message" -> Json.toJson(message)(Message.writes(chat))
    )
    push(0, groupId, json, chat.userIds.contains)
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

  def typing(groupId: Int, userId: Int, chat: Chat) = {
    val json = Json.obj(
      "user_id" -> userId,
      "chat_id" -> chat.id,
      "chat_type" -> chat.chatType
    )
    push(20, groupId, json, chat.userIds.contains)
  }

  def newGroupChat(groupId: Int, userIds: List[Int]) = {
    push(21, groupId, Json.toJson(userIds), userIds.contains)
  }

  def newUserInChat(groupId: Int, chat: Chat, userId: Int) = {
    val json = Json.obj(
      "chat_id" -> chat.id,
      "user_id" -> userId
    )
    push(22, groupId, json, chat.userIds.contains)
  }

  def removeUserInChat(groupId: Int, chat: Chat, userId: Int) = {
    val json = Json.obj(
      "chat_id" -> chat.id,
      "user_id" -> userId
    )
    push(23, groupId, json, chat.userIds.contains)
  }

  def archiveChat(groupId: Int, chat: Chat) = {
    push(24, groupId, Json.obj("chat_id" -> chat.id), chat.userIds.contains)
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
