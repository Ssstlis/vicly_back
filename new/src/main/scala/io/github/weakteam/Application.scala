package io.github.weakteam

import cats.arrow.FunctionK
import cats.data.NonEmptyList
import cats.effect.{Async, Blocker, ConcurrentEffect, ContextShift, ExitCode, IO, IOApp, Resource, Sync, Timer}
import cats.syntax.flatMap._
import cats.syntax.apply._
import cats.syntax.reducible._
import cats.{~>}
import config.AppConfig
import io.github.weakteam.controller.{RoleController, VersionController}
import io.github.weakteam.database.{DbTransactor, Flyway}
import io.github.weakteam.repository.RoleRepository
import io.github.weakteam.service.RoleService
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze._
import tofu.logging.Logs

object Application extends IOApp {

  def mkRouter[F[_]: Sync](roleService: RoleService[F]): HttpRoutes[F] = {
    implicit val dsl: Http4sDsl[F] = new Http4sDsl[F] {}

    Router(
      "/" ->
          NonEmptyList
            .of(
              VersionController.routes[F],
              RoleController.routes[F](roleService)
            )
            .reduceK
    )
  }

  def mkApp[I[_]: Async: ContextShift, F[_]: ConcurrentEffect: Timer: ContextShift](
    fk: F ~> I
  ): Resource[I, F[ExitCode]] = {

    for {
      blocker <- Blocker[I]
      implicit0(logProvider: Logs[I, F]) = Logs.sync[I, F]
      config <- Resource.liftF(AppConfig.load[I](blocker))
      flyway <- Resource.liftF(Flyway.create[I, F](config.db))
      trans <- DbTransactor.resource[F](config.db).mapK(fk)
      roleRep <- Resource.liftF(RoleRepository[I, F](trans, logProvider))
      roleService <- Resource.liftF(RoleService[I, F, F](roleRep, FunctionK.id, logProvider))
    } yield {
      val router = mkRouter[F](roleService).orNotFound
      Sync[F].delay(println(config)) *>
        flyway.flatMap(_.migrate) *>
        BlazeServerBuilder[F]
          .bindHttp(8080, "localhost")
          .withHttpApp(router)
          .withNio2(true)
          .serve
          .compile
          .lastOrError
    }
  }

  override def run(args: List[String]): IO[ExitCode] = {
    mkApp[IO, IO](FunctionK.id).use(IO.suspend(_))
  }
}
