package io.github.weakteam.route

import cats.effect.Sync
import cats.syntax.applicative._
import io.circe.Encoder._
import io.circe.syntax._
import io.circe.{Json, JsonObject}
import io.github.weakteam.BuildInfo
import io.github.weakteam.util.http4s.ResponseOps
import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

object VersionRoutes {
  def routes[F[_]: Sync](implicit dsl: Http4sDsl[F]): HttpRoutes[F] = {
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "version" =>
        val json = Json.fromJsonObject(
          JsonObject(
            "name" -> BuildInfo.name.asJson,
            "version" -> BuildInfo.version.asJson,
            "commit" -> BuildInfo.buildCommit.asJson,
            "branch" -> BuildInfo.buildBranch.asJson,
            "time" -> BuildInfo.buildTime.asJson,
            "buildNumber" -> BuildInfo.buildNumber.asJson
          )
        )
        ResponseOps.Ok[F](json).pure[F]
    }
  }
}
