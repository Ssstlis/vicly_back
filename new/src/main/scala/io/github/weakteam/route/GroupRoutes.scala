package io.github.weakteam.route

import cats.effect.Sync
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object GroupRoutes {
  def routes[F[_]: Sync](implicit dsl: Http4sDsl[F]): HttpRoutes[F] = {
    import dsl._
    val root = Root / "group"
    HttpRoutes.of[F] {
      case POST -> `root`                 => ???
      case POST -> `root` / "set_purpose" => ???
      case GET -> `root` / "list"         => ???
    }
  }
}
