package services

import akka.actor.ActorRef
import com.google.inject.{Inject, Singleton}
import models.{Chat, Message}
import org.bson.types.ObjectId
import play.api.libs.json.{JsValue, Json}
import utils.JsonHelper.ObjectIdFormat

@Singleton
class SocketNotificationService @Inject()(subscriberService: SubscriberService) {

  private def push(
    event: Int,
    groupId: Int,
    json: JsValue,
    filter: Int => Boolean = (_: Int) => true,
    subs: Int => List[(ActorRef, Int)] = (groupId: Int) => subscriberService.allSubscribers
  ) = {
    subs(groupId).collect { case (subscriber, userId) if filter(userId) =>
      subscriber ! Json.obj(
        "event" -> event,
        "message" -> json
      )
    }
  }

  def markRead(id: ObjectId, chat: Chat, updatedMessage: Message) = {
    push(5, 0, Json.obj("message_id" -> id, "chat" -> chat, "message" -> updatedMessage), chat.userIds.contains,
      (_: Int) => subscriberService.allSubscribers)
  }

  def markDelivery(id: ObjectId, chat: Chat, updatedMessage: Message) = {
    push(4, 0, Json.obj("message_id" -> id, "chat" -> chat, "message" -> updatedMessage), chat.userIds.contains,
      (_: Int) => subscriberService.allSubscribers)
  }

  def changed(groupId: Int, id: ObjectId, userIds: List[Int]) = {
    push(3, groupId, Json.obj("id" -> id), userIds.contains,
      (_: Int) => subscriberService.allSubscribers)
  }

  def softDelete(groupId: Int, id: ObjectId, userIds: List[Int]) = {
    push(2, groupId, Json.obj("id" -> id), userIds.contains,
      (_: Int) => subscriberService.allSubscribers)
  }

  def remove(groupId: Int, id: ObjectId, userIds: List[Int]) = {
    push(1, groupId, Json.obj("id" -> id), userIds.contains, (_: Int) => subscriberService.allSubscribers)
  }

  // TODO need rework of subscribers map For get more control
  def newMessage(message: Message, chat: Chat) = {
    val json = Json.obj(
      "message" -> Json.toJson(message)(Message.writes(chat))
    )
    push(0, 0, json, chat.userIds.contains,
      (_: Int) => subscriberService.allSubscribers)
  }

  def online(groupIdO: Option[Int], userId: Int) = {
    push(11, 0, Json.obj("id" -> userId))
  }

  def offline(groupIdO: Option[Int], userId: Int) = {
    push(12, 0, Json.obj("id" -> userId))
  }

  def archive(groupIdO: Option[Int], userId: Int) = {
    push(13, 0, Json.obj("id" -> userId))
  }

  def typing(groupId: Int, userId: Int, chat: Chat) = {
    val json = Json.obj(
      "user_id" -> userId,
      "chat_id" -> chat.id
    )
    push(20, groupId, json, chat.userIds.contains,
      (_: Int) => subscriberService.allSubscribers)
  }

  def newGroupChat(chat:Chat) = {
    push(21, 0, Json.toJson(chat), chat.userIds.contains,
      (_: Int) => subscriberService.allSubscribers)
  }

  def newUserInChat(chat: Chat, userId: Int) = {
    val json = Json.obj(
      "chat_id" -> chat.id,
      "user_id" -> userId
    )
    push(22, 0, json, chat.userIds.contains,
      (_: Int) => subscriberService.allSubscribers)
  }

  def newChatForUser(chat: Chat, userId: Int) = {
    val json = Json.obj(
      "chat" -> chat,
    )
    push(25, 0, json, id => id == userId)
  }

  def removeUserInChat(groupId: Int, chat: Chat, userId: Int) = {
    val json = Json.obj(
      "chat_id" -> chat.id,
      "user_id" -> userId
    )
    push(23, groupId, json, chat.userIds.contains,
      (_: Int) => subscriberService.allSubscribers)
  }

  def archiveChat(groupId: Int, chat: Chat) = {
    push(24, groupId, Json.obj("chat_id" -> chat.id), chat.userIds.contains,
      (_: Int) => subscriberService.allSubscribers)
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

  def userSetAvatar(userId: Int, avatar_id: String) = {
    val json = Json.obj(
      "user_id" -> userId,
      "avatar_id" -> avatar_id
    )
    push(32, 0, json, _ => true,
      (_: Int) => subscriberService.allSubscribers)
  }

  def userRemoveAvatar(groupId: Int, userId: Int) = {
    push(33, 0, Json.obj("user_id" -> userId))
  }
}
