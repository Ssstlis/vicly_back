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
      case req @ POST -> `root`         => controller.add(req)
      case req @ PATCH -> `root` / id   => controller.update(req, id)
      case DELETE -> `root` / id        => controller.delete(id)
      case req @ GET -> `root` / "list" => controller.list(req)
      case GET -> `root` / id           => controller.one(id)
    }
  }
}
