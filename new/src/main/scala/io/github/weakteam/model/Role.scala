package io.github.weakteam.model

import eu.timepit.refined.types.numeric.PosInt
import io.circe.generic.JsonCodec
import io.circe.refined._

@JsonCodec
final case class Role(
  id: PosInt,
  groupId: PosInt,
  description: String
)
