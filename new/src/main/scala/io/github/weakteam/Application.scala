package io.github.weakteam

import cats.arrow.FunctionK
import cats.effect.{Async, Blocker, ConcurrentEffect, ContextShift, ExitCode, IO, IOApp, Resource, Sync, Timer}
import cats.syntax.flatMap._
import cats.syntax.apply._
import cats.~>
import config.AppConfig
import io.github.weakteam.controller.RoleController
import io.github.weakteam.route.{RoleRoutes, VersionRoutes}
import io.github.weakteam.database.{DbTransactor, Flyway}
import io.github.weakteam.repository.RoleRepository
import io.github.weakteam.service.RoleService
import org.http4s.HttpRoutes
import org.http4s.client.Client
import org.http4s.client.middleware.GZip
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze._
import org.http4s.server.middleware.CORS
import tofu.logging.Logs

object Application extends IOApp {

  def mkRouter[F[_]: Sync](roleController: RoleController[F]): HttpRoutes[F] = {
    implicit val dsl: Http4sDsl[F] = new Http4sDsl[F] {}

    Router(
      "/" -> VersionRoutes.routes[F],
      "/api" -> RoleRoutes.routes[F](roleController)
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
      roleController <- Resource.liftF(RoleController[I, F](roleService, logProvider))
    } yield {

      val router = {
        (GZip[F](config.gzip.bufferSizeMultiplier.value * 1024) _)
          .compose(Client.fromHttpApp[F])(
            CORS(
              mkRouter(roleController),
              config.cors.toHttp4sCors
            ).orNotFound
          )
          .toHttpApp
      }

      flyway.flatMap(_.migrate) *>
        BlazeServerBuilder[F]
          .bindHttp(config.http.port.value, config.http.host.value)
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
