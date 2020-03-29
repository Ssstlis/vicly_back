package controllers


import scala.concurrent.Future

import akka.actor.ActorSystem
import akka.stream.Materializer
import actors.WsActor
import com.google.inject.{Inject, Singleton}
import org.bson.types.ObjectId
import pdi.jwt.JwtJson
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.libs.streams.ActorFlow
import play.api.mvc._
import services._
import utils.JsonHelper.ObjectIdFormat

@Singleton
class WsController @Inject()(
  config: ConfigService,
  userService: UserService
)(implicit
  actorSystem: ActorSystem,
  materializer: Materializer,
  socketNotificationService: SocketNotificationService,
  subscriberService: SubscriberService
) extends InjectedController {

  val logger = Logger(classOf[WsController])

  private def decodeToken(token: String) = {
    JwtJson.decodeJson(token, config.secret_key, Seq(config.algo)).toOption
  }

  def subscribe(token: String) = WebSocket.acceptOrResult[JsValue, JsValue] { _ =>
    Future.successful {
      (for {
        json <- decodeToken(token)
        userId <- (json \ "user_id").asOpt[ObjectId]
        login <- (json \ "login").asOpt[String]
        password <- (json \ "password").asOpt[String]
        user <- userService.find(userId, login, password)
        groupId <- user.groupId
      } yield {
        Right {
          logger.info(s"Connect ${user.id} with WS")
          ActorFlow.actorRef { subscriber =>
            subscriberService.addSubscriber(groupId, subscriber, user.id)
            WsActor.props(subscriber, user)
          }
        }
      }).getOrElse(Left(BadRequest))
    }
  }
}
