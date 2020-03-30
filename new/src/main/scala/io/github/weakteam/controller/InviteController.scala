package io.github.weakteam.controller

import cats.effect.Sync
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object InviteController {
  def routes[F[_]: Sync](implicit dsl: Http4sDsl[F]): HttpRoutes[F] = {
    import dsl._
    val root = Root / "invite"
    HttpRoutes.of[F] {
      case POST -> `root`            => ???
      case POST -> `root` / "signup" => ???
      case GET -> `root` / "list"    => ???
      case GET -> `root` / _         => ???
    }
  }
}
