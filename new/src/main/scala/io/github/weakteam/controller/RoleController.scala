package io.github.weakteam.controller

import cats.effect.Sync
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object RoleController {
  def routes[F[_]: Sync](implicit dsl: Http4sDsl[F]): HttpRoutes[F] = {
    import dsl._
    val root = Root / "role"
    HttpRoutes.of[F] {
      case POST -> `root` / _      => ???
      case PATCH -> `root` / _ / _ => ???
      case DELETE -> `root` / _    => ???
      case GET -> `root` / "list"  => ???
      case GET -> `root` / _       => ???
    }
  }
}
