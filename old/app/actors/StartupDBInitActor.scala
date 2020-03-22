package actors

import akka.actor.ActorSystem
import javax.inject.Inject
import models.{Group, User}
import org.bson.types.ObjectId
import services.{GroupService, UserService}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class StartupDBInitActor @Inject()(
  actorSystem: ActorSystem,
  userService: UserService,
  groupService: GroupService
)
  (implicit executionContext: ExecutionContext) {

  actorSystem.scheduler.scheduleOnce(delay = 0.seconds) {
    if (userService.findOne(0).isEmpty) {
      val user = new User(new ObjectId(), "Vicly", None, "", None, None, "", "", false, 0, 0, 0, false, None, 0)
      userService.create(user).wasAcknowledged()
    }
    if (groupService.findById(0).isEmpty) {
      userService.findOne(0).map { systemUser =>
        val group = new Group(new ObjectId(), 0, "common", 0, systemUser._id, None)
        groupService.create(group)
      }
    }
  }
}