package io.github.weakteam.util.circe

import cats.Invariant
import cats.syntax.{ContravariantSyntax, InvariantSyntax}
import io.circe.{Codec, Decoder, Encoder}

object syntax extends InvariantSyntax with ContravariantSyntax {
  implicit val codecInvariant: Invariant[Codec] = new Invariant[Codec] {
    def imap[A, B](fa: Codec[A])(f: A => B)(g: B => A): Codec[B] = {
      Codec.from[B](fa.map(f), fa.contramap(g))
    }
  }

  def deriveCodec[A](implicit E: Encoder[A], D: Decoder[A]): Codec[A] = {
    Codec.from[A](D(_), E(_))
  }
}
