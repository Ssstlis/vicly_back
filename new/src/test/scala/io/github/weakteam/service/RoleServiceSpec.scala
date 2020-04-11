package io.github.weakteam.service

import eu.timepit.refined.types.numeric.PosInt
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class RoleServiceSpec extends AnyWordSpec with Matchers {
  "RoleService#posIngLoggable" should {
    "return right value" in {
      RoleService.posIngLoggable.logShow(PosInt.unsafeFrom(1)) mustBe "1"
    }
  }
}
