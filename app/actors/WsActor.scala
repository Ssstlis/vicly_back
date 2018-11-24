package actors

import akka.actor.{Actor, ActorRef, Props}
import models.User
import play.api.Logger
import play.api.libs.json.Json
import services.{SocketNotificationService, SubscriberService}

object WsActor {
  val logger = Logger(classOf[WsActor])

  def props(subscriber: ActorRef, user: User)
    (implicit subscriberService: SubscriberService, socketNotificationService: SocketNotificationService) = {
    Props(new WsActor(subscriber, subscriberService, socketNotificationService, user))
  }
}

class WsActor(
  subscriber: ActorRef,
  subscriberService: SubscriberService,
  socketNotificationService: SocketNotificationService,
  user: User
) extends Actor {

  import WsActor._

  override def receive: Receive = {
    case _ => subscriber ! Json.obj()
  }

  override def postStop(): Unit = {
    logger.info(s"Disconnect ${user.id} with WS")
    socketNotificationService.offline(user.groupId, user.id)
    user.groupId.foreach(groupId =>
      subscriberService.removeSubscriber(groupId, subscriber)
    )
  }
}
