package io.github.weakteam.route

import cats.effect.Sync
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object AttachmentRoutes {
  def routes[F[_]: Sync](implicit dsl: Http4sDsl[F]): HttpRoutes[F] = {
    import dsl._
    val root = Root / "attachment"
    HttpRoutes.of[F] {
      case POST -> `root`                           => ???
      case POST -> `root` / "avatar"                => ???
      case GET -> `root` / _                        => ???
      case GET -> `root` / "download" / _           => ???
      case GET -> `root` / "download" / _ / "small" => ???
      case GET -> `root` / "download" / _ / "big"   => ???
      case GET -> `root` / "download_avatar" / _    => ???
      case GET -> `root` / "user"                   => ???
      case GET -> `root` / "list"                   => ???
      case DELETE -> `root` / _                     => ???
    }
  }
}
