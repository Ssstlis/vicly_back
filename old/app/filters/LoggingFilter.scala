package filters

import scala.concurrent.{ExecutionContext, Future}

import akka.stream.Materializer
import com.google.inject.Inject
import play.api.Logger
import play.api.mvc._
import utils.LoggingHelper._

class LoggingFilter @Inject()(
  implicit ec: ExecutionContext,
  val mat: Materializer
) extends Filter {

  implicit val logger = Logger(classOf[LoggingFilter])

  override def apply(nextFilter: RequestHeader => Future[Result])(request: RequestHeader): Future[Result] = {
    val startTime = System.currentTimeMillis
    nextFilter(request).map { loggingRequest(startTime, request) }
  }
}