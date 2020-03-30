package io.github.weakteam.controller

import cats.effect.Sync
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object ChatController {
  def routes[F[_]: Sync](implicit dsl: Http4sDsl[F]): HttpRoutes[F] = {
    import dsl._
    val root = Root / "chat"
    HttpRoutes.of[F] {
      case POST -> `root`              => ???
      case PATCH -> `root` / "typing"  => ???
      case PATCH -> `root` / "add"     => ???
      case PATCH -> `root` / "archive" => ???
      case DELETE -> `root`            => ???
    }
  }
}
