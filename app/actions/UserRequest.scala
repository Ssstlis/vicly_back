package actions

import models.User
import play.api.mvc.Request

case class UserRequest[A](user: User, request: Request[A])
  extends RequestApi[A](request)