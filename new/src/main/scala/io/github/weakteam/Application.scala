package io.github.weakteam

import cats.effect.{Blocker, ConcurrentEffect, ContextShift, ExitCode, IO, IOApp, Resource, Sync, Timer}
import cats.syntax.functor._
import cats.syntax.apply._
import config.AppConfig
import io.github.weakteam.controller.VersionController
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze._

object Application extends IOApp {

  def mkRouter[F[_]: Sync]: HttpRoutes[F] = {
    implicit val dsl: Http4sDsl[F] = new Http4sDsl[F] {}

    Router("/" -> VersionController.routes[F])
  }

  def mkApp[F[_]: ConcurrentEffect: Timer: ContextShift]: F[ExitCode] = {

    val router = mkRouter[F].orNotFound

    (for {
      blocker <- Blocker[F]
      config <- Resource.liftF(AppConfig.load[F](blocker))
    } yield config).use { config =>
      Sync[F].delay(println(config)) *>
        BlazeServerBuilder[F]
          .bindHttp(8080, "localhost")
          .withHttpApp(router)
          .withNio2(true)
          .serve
          .compile
          .drain
          .as(ExitCode.Success)
    }
  }

  override def run(args: List[String]): IO[ExitCode] = {
    mkApp[IO]
  }
}
