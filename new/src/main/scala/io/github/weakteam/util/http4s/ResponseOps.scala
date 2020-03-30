package io.github.weakteam.util.http4s

import cats.Applicative
import org.http4s.{EntityEncoder, Response, Status}

object ResponseOps {
  trait ResponseOps[F[_]] {
    def apply[A: EntityEncoder[F, *]](body: A)(implicit F: Applicative[F]): F[Response[F]]
  }

  private[ResponseOps] final class StatusPartialApplied[F[_]](status: Status) extends ResponseOps[F] {
    def apply[A: EntityEncoder[F, *]](body: A)(implicit F: Applicative[F]): F[Response[F]] =
      F.pure(Response[F](status).withEntity(body))
  }

  def Ok[F[_]]: ResponseOps[F] = new StatusPartialApplied[F](Status.Ok)

  def BadRequest[F[_]]: ResponseOps[F] = new StatusPartialApplied[F](Status.BadRequest)
}
