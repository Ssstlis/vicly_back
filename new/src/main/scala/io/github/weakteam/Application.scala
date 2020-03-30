package io.github.weakteam

import cats.effect.{ConcurrentEffect, ExitCode, IO, IOApp, Sync, Timer}
import cats.syntax.apply._
import cats.syntax.functor._
import io.github.weakteam.controller.HelloController
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze._
import tofu.common.Console

object Application extends IOApp {

  def mkRouter[F[_]: Sync]: HttpRoutes[F] = {
    implicit val dsl: Http4sDsl[F] = new Http4sDsl[F] {}

    Router("/hello" -> HelloController.mkHello[F])
  }

  def mkApp[F[_]: ConcurrentEffect: Timer]: F[ExitCode] = {

    val router = mkRouter[F].orNotFound

    BlazeServerBuilder[F]
      .bindHttp(8080, "localhost")
      .withHttpApp(router)
      .withNio2(true)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
  }

  override def run(args: List[String]): IO[ExitCode] = {
    Console.apply[IO].putStrLn("Hello, dude") *> mkApp[IO]
  }
}
