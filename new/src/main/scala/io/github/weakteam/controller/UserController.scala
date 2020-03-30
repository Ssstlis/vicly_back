package io.github.weakteam.controller

import cats.effect.Sync
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object UserController {
  def routes[F[_]: Sync](implicit dsl: Http4sDsl[F]): HttpRoutes[F] = {
    import dsl._
    val root = Root / "user"
    HttpRoutes.of[F] {
      case POST -> `root` / "login"           => ???
      case GET -> `root` / "logout"           => ???
      case GET -> `root` / "list"             => ???
      case GET -> `root` / "update_password"  => ???
      case PATCH -> `root` / "archive"        => ???
      case PATCH -> `root` / "set_status" / _ => ???
      case PATCH -> `root` / "clear_status"   => ???
      case PATCH -> `root` / "remove_avatar"  => ???
    }
  }
}
