package io.github.weakteam.route

import cats.effect.Sync
import io.github.weakteam.controller.RoleController
import org.http4s._
import org.http4s.dsl.Http4sDsl

object RoleRoutes {

  def routes[F[_]: Sync](controller: RoleController[F])(implicit dsl: Http4sDsl[F]): HttpRoutes[F] = {
    import dsl._
    val root = Root / "role"
    HttpRoutes.of[F] {
      case POST -> `root` / _      => ???
      case PATCH -> `root` / _ / _ => ???
      case DELETE -> `root` / _    => ???
      case GET -> `root` / "list"  => controller.list
      case GET -> `root` / _       => ???
    }
  }
}
