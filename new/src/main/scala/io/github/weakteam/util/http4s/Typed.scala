package io.github.weakteam.util.http4s

import fs2.Stream
import io.circe.Encoder
import org.http4s.circe.jsonEncoderOf

object Typed {
  def streamedPipeEncoder[F[_], T](implicit E: Encoder[T]): T => Stream[F, Byte] = jsonEncoderOf(E).toEntity(_).body
}
