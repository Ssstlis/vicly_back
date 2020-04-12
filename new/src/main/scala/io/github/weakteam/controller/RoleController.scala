package io.github.weakteam.controller

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.github.ghik.silencer.silent
import eu.timepit.refined.numeric.Positive
import io.circe.syntax._
import io.circe.{Decoder, Json}
import io.github.weakteam.model.Role
import io.github.weakteam.service.RoleService
import io.github.weakteam.util.Typed._
import io.github.weakteam.util.http4s.ResponseOps
import io.github.weakteam.util.http4s.Typed._
import org.http4s.circe._
import org.http4s.headers.`Content-Type`
import org.http4s.{MediaType, Request, Response, Status}
import tofu.logging.{Logging, LoggingBase, Logs}

trait RoleController[F[_]] {
  def one(id: String): F[Response[F]]
  def list(request: Request[F]): F[Response[F]]
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

    val en: fs2.Stream[F, Role] => fs2.Stream[F, Byte] = streamedPipeEncoder[F, Role]

    def one(id: String): F[Response[F]] =
      for {
        int <- mapMInt[F](id) >>= (refineMonadic[F, Positive](_))
        role <- service.findOne(int)
      } yield {
        role match {
          case Some(value) => ResponseOps.Ok[F](value.asJson)
          case _           => ResponseOps.BadRequest[F](Json.obj("error" := List(s"Role with id $int not found.")))
        }
      }

    def list(request: Request[F]): F[Response[F]] = {
      findOneOptPositiveInt[F]("lastKey")
        .apply(request)
        .flatMap { lastKey =>
          service.findAllPaginated(lastKey).map { s =>
            ResponseOps.Ok[F](en(s)).withContentType(`Content-Type`(MediaType.application.json))
          }
        }
    }

    def add(request: Request[F]): F[Response[F]] =
      for {
        role <- request.attemptAs[Json].subflatMap(Decoder[Role].decodeJson(_)).rethrowT
        insert <- service.insert(role)
      } yield {
        Response[F](if (insert == 1) Status.Created else Status.NotModified)
      }

    def delete(id: String): F[Response[F]] =
      for {
        int <- mapMInt[F](id) >>= (refineMonadic[F, Positive](_))
        removed <- service.remove(int)
      } yield {
        Response[F](if (removed == 1) Status.Ok else Status.NotModified)
      }

    def update(request: Request[F], id: String): F[Response[F]] =
      for {
        //Forward changes by models
        _ <- mapMInt[F](id) >>= (refineMonadic[F, Positive](_))
        role <- request.attemptAs[Json].subflatMap(Decoder[Role].decodeJson(_)).rethrowT
        updated <- service.update(role)
      } yield {
        Response[F](if (updated == 1) Status.Ok else Status.NotModified)
      }
  }

}
