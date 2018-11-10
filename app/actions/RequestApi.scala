package actions

import play.api.mvc.{Request, WrappedRequest}

class RequestApi[+A](request: Request[A])
  extends WrappedRequest[A](request)