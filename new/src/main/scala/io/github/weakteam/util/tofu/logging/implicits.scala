package io.github.weakteam.util.tofu.logging

import eu.timepit.refined.types.numeric.PosInt
import tofu.logging.Loggable

object implicits {
  implicit val posIngLoggable: Loggable[PosInt] = Loggable.intLoggable.contramap[PosInt](_.value)
}
