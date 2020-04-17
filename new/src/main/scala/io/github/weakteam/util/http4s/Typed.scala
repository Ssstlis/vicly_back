package io.github.weakteam.util.http4s

import cats.MonadError
import cats.instances.option._
import cats.syntax.flatMap._
import cats.syntax.traverse._
import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.types.numeric.{PosInt, PosLong}
import io.github.weakteam.util.Typed._
import org.http4s.Request

object Typed {

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

  def findOneOptPositiveLong[G[_]](
    key: String
  )(implicit F: MonadError[G, _ >: RuntimeException]): Request[G] => G[Option[PosLong]] = {
    findOneOptFromReqAndRefine[G, Long, Positive](key, mapMLong[G](_))
  }
}
