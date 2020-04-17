package io.github.weakteam.util.refined

import cats.Show
import cats.instances.long.catsStdShowForLong
import cats.syntax.contravariant.toContravariantOps
import eu.timepit.refined.types.numeric.PosLong

object implicits {
  implicit val showPosLong: Show[PosLong] = Show[Long].contramap(_.value)
}
