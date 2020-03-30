package io.github.weakteam.controller

import cats.effect.Sync
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object MessageController {
  def routes[F[_]: Sync](implicit dsl: Http4sDsl[F]): HttpRoutes[F] = {
    import dsl._
    val root = Root / "message"
    HttpRoutes.of[F] {
      //post
      case POST -> `root`                  => ???
      case POST -> `root` / "read" / _     => ???
      case POST -> `root` / "delivery" / _ => ???
      //edit
      case PATCH -> `root` / _ => ???
      //delete
      case DELETE -> `root` / _       => ???
      case GET -> `root` / "from" / _ => ???
    }
  }
}
