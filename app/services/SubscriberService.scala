package services

import scala.collection.mutable

import akka.actor.ActorRef
import com.google.inject.Singleton

@Singleton
class SubscriberService {

  private val subscribersMap = mutable.Map.empty[Int, mutable.Buffer[(ActorRef, Int)]]

  private def addSubscriber_(groupId: Int, subscriber: ActorRef, userId: Int): Unit = {
    val newSubscribers = {
      subscribersMap.getOrElse(groupId, mutable.Buffer.empty) += (subscriber -> userId)
    }

    subscribersMap += (groupId -> newSubscribers)
  }

  private def removeSubscriber_(groupId: Int, subscriber: ActorRef, userId: Int): Unit = {
    subscribersMap.get(groupId).map { subscribers =>
      subscribers -= (subscriber -> userId)
      subscribersMap += (groupId -> subscribers)
    }
  }

  private def subscriptionSubscribers_(groupId: Int) = {
    subscribersMap.getOrElse(groupId, mutable.Buffer.empty).toList
  }

  def subscriptionSubscribers(groupId: Int) = {
    subscriptionSubscribers_(groupId)
  }

  def addSubscriber(groupId: Int, subscriber: ActorRef, userId: Int): Unit = {
    addSubscriber_(groupId, subscriber, userId)
  }

  def removeSubscriber(groupId: Int, subscriber: ActorRef, userId: Int): Unit = {
    removeSubscriber_(groupId, subscriber, userId)
  }
}
