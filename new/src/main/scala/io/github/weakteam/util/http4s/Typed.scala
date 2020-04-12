package io.github.weakteam.util.http4s

import cats.MonadError
import cats.instances.option._
import cats.syntax.flatMap._
import cats.syntax.traverse._
import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.types.numeric.PosInt
import fs2.Stream
import io.circe.Encoder
import io.github.weakteam.util.Typed._
import org.http4s.Request
import org.http4s.circe.jsonEncoderOf

object Typed {
  def streamedPipeEncoder[F[_], T](implicit E: Encoder[T]): Stream[F, T] => Stream[F, Byte] =
    _.flatMap(jsonEncoderOf(E).toEntity(_).body)

  def findOneOptFromReqAndRefine[G[_], T, R: Validate[T, *]](
    key: String,
    f: String => G[T]
  )(implicit F: MonadError[G, _ >: RuntimeException]): Request[G] => G[Option[T Refined R]] =
    _.multiParams
      .get(key)
      .flatMap(_.headOption)
      .traverse(f)
      .flatMap(_.traverse(refineMonadic[G, R](_)))

  def findOneOptPositiveInt[G[_]](
    key: String
  )(implicit F: MonadError[G, _ >: RuntimeException]): Request[G] => G[Option[PosInt]] = {
    findOneOptFromReqAndRefine[G, Int, Positive](key, mapMInt[G](_))
  }
}
