package io.github.weakteam.controller

import cats.effect.Sync
import cats.syntax.functor._
import fs2.Stream
import io.circe.Encoder
import io.github.weakteam.model.Role
import io.github.weakteam.service.RoleService
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`

object RoleController {

  def streamedPipeEncoder[F[_], T](implicit E: Encoder[T]): T => Stream[F, Byte] = jsonEncoderOf(E).toEntity(_).body

  def routes[F[_]: Sync](service: RoleService[F])(implicit dsl: Http4sDsl[F]): HttpRoutes[F] = {
    import dsl._
    val root = Root / "role"
    HttpRoutes.of[F] {
      case POST -> `root` / _      => ???
      case PATCH -> `root` / _ / _ => ???
      case DELETE -> `root` / _    => ???
      case GET -> `root` / "list" =>
        val en = streamedPipeEncoder[F, Role]
        service.findAllPaginated(None).map { s =>
          Response[F](Status.Ok)
            .withBodyStream(s.flatMap(en))
            .withContentType(`Content-Type`(MediaType.application.json))
        }
      case GET -> `root` / _ => ???
    }
  }
}
