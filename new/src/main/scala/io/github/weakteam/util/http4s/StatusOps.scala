package io.github.weakteam.util.http4s

import cats.Applicative
import org.http4s.{EntityEncoder, Response, Status}

object StatusOps {
  trait StatusOps[F[_]] {
    def apply[A: EntityEncoder[F, *]](body: A)(implicit F: Applicative[F]): F[Response[F]]
  }

  private[StatusOps] final class StatusPartialApplied[F[_]](status: Status) extends StatusOps[F] {
    def apply[A: EntityEncoder[F, *]](body: A)(implicit F: Applicative[F]): F[Response[F]] =
      F.pure(Response[F](status = status, body = EntityEncoder[F, A].toEntity(body).body))
  }

  def Ok[F[_]]: StatusOps[F] = new StatusPartialApplied[F](Status.Ok)

  def BadRequest[F[_]]: StatusOps[F] = new StatusPartialApplied[F](Status.BadRequest)
}
