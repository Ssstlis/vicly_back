package io.github.weakteam.util.http4s

import org.http4s.{DecodeFailure, EntityEncoder}

object implicits {
  implicit def decodeFailureEntityEncoder[F[_]]: EntityEncoder[F, DecodeFailure] =
    EntityEncoder.stringEncoder[F].contramap(_.toString)
}
