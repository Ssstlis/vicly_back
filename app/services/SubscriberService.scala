package services

import javax.inject.Inject

import scala.collection.mutable

import akka.actor.ActorRef
import com.google.inject.Singleton

@Singleton
class SubscriberService @Inject()() {

  private val subscribersMap = mutable.Map.empty[Int, mutable.Buffer[ActorRef]]

  private def addSubscriber_(groupId: Int, subscriber: ActorRef): Unit = {
    val newSubscribers = {
      subscribersMap.getOrElse(groupId, mutable.Buffer.empty) += subscriber
    }

    subscribersMap += (groupId -> newSubscribers)
  }

  private def removeSubscriber_(groupId: Int, subscriber: ActorRef): Unit = {
    subscribersMap.get(groupId).map { subscribers =>
      subscribers -= subscriber
      subscribersMap += (groupId -> subscribers)
    }
  }

  private def subscriptionSubscribers_(groupId: Int) = {
    subscribersMap.getOrElse(groupId, mutable.Buffer.empty).toList
  }

  def subscriptionSubscribers(groupId: Int) = {
    subscriptionSubscribers_(groupId)
  }

  def addSubscriber(groupId: Int, subscriber: ActorRef): Unit = {
    addSubscriber_(groupId, subscriber)
  }

  def removeSubscriber(groupId: Int, subscriber: ActorRef): Unit = {
    removeSubscriber_(groupId, subscriber)
  }
}
