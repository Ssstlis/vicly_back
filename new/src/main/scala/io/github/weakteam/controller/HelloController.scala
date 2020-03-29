package io.github.weakteam.controller

import cats.effect.Sync
import io.circe.Json
import io.github.weakteam.util.http4s.StatusOps
import io.github.weakteam.util.http4s.implicits._
import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

object HelloController {
  def mkHello[F[_]: Sync](implicit dsl: Http4sDsl[F]): HttpRoutes[F] = {
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "hello" / name =>
        StatusOps.Ok(s"Hello, $name.")
      case body @ POST -> Root =>
        body
          .attemptAs[Json]
          .foldF(
            StatusOps.BadRequest[F](_),
            StatusOps.Ok[F](_)
          )
    }
  }
}
