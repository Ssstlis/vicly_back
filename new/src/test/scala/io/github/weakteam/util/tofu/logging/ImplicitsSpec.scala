package io.github.weakteam.util.tofu.logging

import eu.timepit.refined.types.numeric.PosInt
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ImplicitsSpec extends AnyWordSpec with Matchers {
  "Tofu-logging-implicits#posIngLoggable" should {
    "return right value" in {
      implicits.posIngLoggable.logShow(PosInt.unsafeFrom(1)) mustBe "1"
    }
  }
}
