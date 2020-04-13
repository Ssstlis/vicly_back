package io.github.weakteam.controller

import cats.effect.Sync
import cats.syntax.functor._
import com.github.ghik.silencer.silent
import io.github.weakteam.model.Role
import io.github.weakteam.service.RoleService
import io.github.weakteam.util.http4s.ResponseOps
import io.github.weakteam.util.http4s.Typed._
import org.http4s.headers.`Content-Type`
import org.http4s.{MediaType, Request, Response}
import tofu.logging.{Logging, LoggingBase, Logs}

trait RoleController[F[_]] {
  def one(id: String): F[Response[F]]
  def list: F[Response[F]]
  def add(request: Request[F]): F[Response[F]]
  def delete(id: String): F[Response[F]]
  def update(request: Request[F], id: String): F[Response[F]]
}

object RoleController {
  def apply[I[_]: Sync, F[_]: Sync](service: RoleService[F], logs: Logs[I, F]): I[RoleController[F]] = {
    for {
      implicit0(logger: Logging[F]) <- logs.forService[RoleController[F]]
    } yield new Impl[F](service)
  }

  private final class Impl[F[_]: Sync](
    service: RoleService[F]
  )(implicit @silent logger: LoggingBase[F])
    extends RoleController[F] {

    val en: Role => fs2.Stream[F, Byte] = streamedPipeEncoder[F, Role]

    def one(id: String): F[Response[F]] = ???

    def list: F[Response[F]] = {
      service.findAllPaginated(None).map { s =>
        ResponseOps.Ok[F](s.flatMap(en)).withContentType(`Content-Type`(MediaType.application.json))
      }
    }

    def add(request: Request[F]): F[Response[F]] = ???

    def delete(id: String): F[Response[F]] = ???

    def update(request: Request[F], id: String): F[Response[F]] = ???
  }
}
