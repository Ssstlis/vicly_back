package io.github.weakteam.controller

import cats.effect.Sync
import io.circe.Json
import io.github.weakteam.util.http4s.ResponseOps
import io.github.weakteam.util.http4s.implicits._
import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

object HelloController {
  def mkHello[F[_]: Sync](implicit dsl: Http4sDsl[F]): HttpRoutes[F] = {
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "hello" / name =>
        ResponseOps.Ok(s"Hello, $name.")
      case body @ POST -> Root =>
        body
          .attemptAs[Json]
          .foldF(
            ResponseOps.BadRequest[F](_),
            ResponseOps.Ok[F](_)
          )
    }
  }
}
