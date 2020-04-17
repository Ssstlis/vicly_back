package io.github.weakteam.model

import cats.Show
import cats.syntax.show._
import cats.instances.either._
import cats.syntax.apply._
import io.circe.{Decoder, Encoder, Json}

final case class WithId[Id, E](id: Id, entity: E)

object WithId {
  implicit def encoderForWithId[I, E](implicit IE: Encoder[I], EE: Encoder[E]): Encoder[WithId[I, E]] = {
    case WithId(id, entity) => Json.obj("id" -> IE(id)).deepMerge(EE(entity))
  }

  implicit def decoderForWithId[I, E](implicit IE: Decoder[I], EE: Decoder[E]): Decoder[WithId[I, E]] = { cursor =>
    cursor.get[I]("id").map2(cursor.as[E])(WithId(_, _))
  }

  implicit def withIdShow[I: Show, E: Show]: Show[WithId[I, E]] = {
    case WithId(id, entity) => show"WithId(id = $id, entity = $entity)"
  }
}
