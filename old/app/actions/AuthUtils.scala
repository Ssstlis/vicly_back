package actions

import ch.qos.logback.core.status.ErrorStatus

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds
import com.google.inject.{Inject, Singleton}
import org.bson.types.ObjectId
import pdi.jwt.JwtJson
import play.api.mvc._
import services.{ConfigService, UserService}
import utils.JsonHelper.ObjectIdFormat

@Singleton
class AuthUtils @Inject()(
  config: ConfigService,
  defaultParser: BodyParsers.Default,
  userService: UserService
)(implicit ec: ExecutionContext) {

  abstract class AbstractActionBuilder[T[_]] extends ActionBuilder[T, AnyContent] {
    override protected def executionContext = ec

    override def parser = defaultParser
  }

  abstract class AbstractActionRefiner[T[_], R[_]] extends ActionRefiner[T, R] {
    override protected def executionContext = ec
  }

  abstract class AbstractActionFilter[T[_]] extends ActionFilter[T] {
    override protected def executionContext = ec
  }



  val authenticateAction: AbstractActionBuilder[UserRequest] = new AbstractActionBuilder[UserRequest] {
    def decodeToken(token: String) = {
      JwtJson.decodeJson(token, config.secret_key, Seq(config.algo)).toOption
    }

    override def invokeBlock[A](
      request: Request[A],
      block: UserRequest[A] => Future[Result]
    ) = {
      (for {
        token <- request.headers.get("Authorization")
        json <- decodeToken(token)
        userId <- (json \ "user_id").asOpt[ObjectId]
        login <- (json \ "login").asOpt[String]
        password <- (json \ "password").asOpt[String]
        user <- userService.find(userId, login, password)
      } yield {
        userService.updateActivity(user.id)(user.groupId)
        block(UserRequest(user, request))
      }).getOrElse(Future.successful(Results.Forbidden))
    }
  }

  val authenticateFromURLAction: AbstractActionBuilder[UserRequest] = new AbstractActionBuilder[UserRequest] {
    def decodeToken(token: String) = {
      JwtJson.decodeJson(token, config.secret_key, Seq(config.algo)).toOption
    }

    override def invokeBlock[A](
      request: Request[A],
      block: UserRequest[A] => Future[Result]
    ) = {
      (for {
        token <- request.target.queryMap.get("token")
        json <- decodeToken(token.head)
        userId <- (json \ "user_id").asOpt[ObjectId]
        login <- (json \ "login").asOpt[String]
        password <- (json \ "password").asOpt[String]
        user <- userService.find(userId, login, password)
      } yield {
        userService.updateActivity(user.id)(user.groupId)
        block(UserRequest(user, request))
      }).getOrElse(Future.successful(Results.Forbidden))
    }
  }
}