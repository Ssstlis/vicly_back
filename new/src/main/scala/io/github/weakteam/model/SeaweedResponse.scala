package io.github.weakteam.model

import cats.Show
import doobie.util.meta.Meta
import doobie.refined.implicits._
import eu.timepit.refined.types.numeric.PosLong
import io.circe.Codec
import io.circe.generic.JsonCodec
import io.circe.refined._
import io.estatico.newtype.macros.newtype
import io.github.weakteam.util.circe.syntax._
import io.github.weakteam.util.refined.implicits._

@JsonCodec
final case class SeaweedResponse(
  eTag: String,
  fileId: PosLong,
  fileName: String,
  fileUrl: String,
  fileSize: PosLong
)

object SeaweedResponse {

  @newtype
  final case class SeaweedResponseId(id: PosLong)

  object SeaweedResponseId {
    implicit val meta: Meta[SeaweedResponseId]   = Meta[PosLong].imap(SeaweedResponseId(_))(_.id)
    implicit val show: Show[SeaweedResponseId]   = Show[PosLong].contramap(_.id)
    implicit val codec: Codec[SeaweedResponseId] = deriveCodec[PosLong].imap(SeaweedResponseId(_))(_.id)
  }

  type RichSeaweedResponse = WithId[SeaweedResponseId, SeaweedResponse]
}
