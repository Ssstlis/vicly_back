package io.github.weakteam.controller

import cats.effect.Sync
import io.circe.Json
import io.github.weakteam.util.http4s.StatusOps
import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.io.{->, /, GET, POST, Root}

object HelloController {
  def mkHello[F[_]: Sync]: HttpRoutes[F] = {
    HttpRoutes.of[F] {
      case GET -> Root / "hello" / name =>
        StatusOps.Ok(s"Hello, $name.")
      case body @ POST -> Root =>
        body
          .attemptAs[Json]
          .foldF(
            failure => StatusOps.BadRequest[F](failure.toString),
            StatusOps.Ok[F](_)
          )
    }
  }
}
