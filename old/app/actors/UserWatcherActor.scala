package actors

import scala.concurrent.duration._

import akka.actor.Actor
import com.google.inject.{Inject, Singleton}
import services.{SocketNotificationService, UserService}

object UserWatcherActor {
  case object Watch
}

@Singleton
class UserWatcherActor @Inject()(
  socketNotificationService: SocketNotificationService,
  userService: UserService
) extends Actor {

  import UserWatcherActor._
  import context._

  system.scheduler.schedule(0.seconds, 15.seconds, self, Watch)

  override def receive: Receive = {
    case Watch => {
      userService.findAllPossiblyOfflined.groupBy(_.groupId).foreach { case (groupIdO, user) =>
        user.foreach { user =>
          userService.setInactive(user)
          socketNotificationService.offline(groupIdO, user.id)
        }
      }
    }
  }
}
