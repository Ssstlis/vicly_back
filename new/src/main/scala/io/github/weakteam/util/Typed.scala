package io.github.weakteam.util

import cats.syntax.either._
import cats.ApplicativeError
import eu.timepit.refined
import eu.timepit.refined.api.{Refined, Validate}

object Typed {
  final class RefinedMonadicPartialApplied[M[_], R] {
    def apply[T: Validate[*, R]](value: T)(implicit F: ApplicativeError[M, _ >: RuntimeException]): M[T Refined R] = {
      refined
        .refineV[R](value)
        .leftMap(s => new RuntimeException(s"""Value "$value" is failed cause by $s"""))
        .liftTo[M]
    }
  }

  final class MapMonadicPartialApplied[M[_]](v: String) {
    def apply[T, E](f: String => Either[E, T])(implicit F: ApplicativeError[M, _ >: E]): M[T] = {
      f(v).liftTo[M]
    }
  }

  def refineMonadic[M[_], R]: RefinedMonadicPartialApplied[M, R] = new RefinedMonadicPartialApplied[M, R]

  def mapMInt[M[_]](s: String)(implicit F: ApplicativeError[M, _ >: RuntimeException]) =
    new MapMonadicPartialApplied[M](s)(
      s =>
        try {
          Right(s.toInt)
        } catch {
          case _: NumberFormatException =>
            Left(new RuntimeException(s"""Required valid int, actual value: "$s""""))
        }
    )
}
