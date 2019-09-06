import cats.data.EitherT

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}
//val a = "video/png"
//val rImage = "^image/(.*)".r
//val rVideo = "^video/(.*)".r
//a match {
//  case rImage(_) => println("Image")
//  case rVideo(_) => println("Video")
//  case _ => println("Other")
//}
