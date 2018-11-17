package utils

import play.api.Logger
import play.api.mvc.{RequestHeader, Result}

object LoggingHelper {

  def loggingRequest(startTime: Long, request: RequestHeader)(result: Result)(implicit logger: Logger) = {
    val endTime = System.currentTimeMillis
    val requestTime = endTime - startTime
    val protocol = if (request.secure) "https://" else "http://"
    logger.info(
      s"""
   |[HTTP Filter]
   |Method: ${request.method}
   |Route: $protocol${request.host}${request.path}
   |From: ${request.headers.get("Remote-Address").getOrElse("No address specified")}
   |To: ${request.host}
   |Agent: ${request.headers.get("User-Agent").getOrElse("No specified agent")}
   |Status: ${result.header.status}
   |Content-length: ${result.body.contentLength.getOrElse(0)}
   |Time to execute(ms): $requestTime"""
    )
    result
  }
}
