package io.github.weakteam.controller

import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.effect.{Effect, IO}
import cats.effect.syntax.effect._
import eu.timepit.refined.types.numeric.PosInt
import fs2.Stream
import io.circe.Json
import io.circe.syntax._
import io.github.weakteam.model.Role
import io.github.weakteam.service.RoleService
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import tofu.logging.Logs
import tofu.syntax.foption._
import RoleControllerSpec._
import org.http4s.Status
import org.http4s.circe._

class RoleControllerSpec extends AnyWordSpec with Matchers {
  "RoleController#fineOne" should {
    "return Ok" in withController[IO](makeService[IO](findOnef = { id =>
      id.value mustBe 1
      Some(defaultRole).pure[IO]
    })) { ctl =>
      ctl.one("1").map { resp =>
        resp.status mustBe Status.Ok
        resp.attemptAs[Json].value.map(_ mustBe Right(defaultRoleJson))
      }
    }

    "return BadRequest" in withController[IO](makeService[IO](findOnef = { id =>
      id.value mustBe 1
      noneF[IO, Role]
    })) { ctl =>
      ctl.one("1").map { resp =>
        resp.status mustBe Status.BadRequest
        resp.attemptAs[Json].value.map(_ mustBe Right(Json.obj("error" := List(s"Role with id 1 not found."))))
      }
    }

    "return BadRequest0" in withController[IO]() { ctl =>
      ctl.one("0").void.recoverWith {
        case ex: RuntimeException =>
          ex.getMessage mustBe """Value "0" is failed cause by Predicate failed: (0 > 0)."""
          IO.unit
      }
    }

    "return BadRequestS" in withController[IO]() { ctl =>
      ctl.one("t").void.recoverWith {
        case ex: RuntimeException =>
          ex.getMessage mustBe """Required valid int, actual value: "t""""
          IO.unit
      }
    }
  }
}

object RoleControllerSpec {
  val defaultRole: Role     = Role(PosInt.unsafeFrom(1), PosInt.unsafeFrom(1), Some("test"))
  val defaultRoleJson: Json = defaultRole.asJson

  def makeService[F[_]](
    findAll: Option[PosInt] => F[Stream[F, Role]] = null,
    findOnef: PosInt => F[Option[Role]] = null,
    insertf: Role => F[Int] = null,
    updatef: Role => F[Int] = null,
    removef: PosInt => F[Int] = null
  ): RoleService[F] = new RoleService[F] {
    def findAllPaginated(lastKey: Option[PosInt]): F[Stream[F, Role]] = findAll(lastKey)
    def findOne(key: PosInt): F[Option[Role]]                         = findOnef(key)
    def insert(role: Role): F[Int]                                    = insertf(role)
    def update(role: Role): F[Int]                                    = updatef(role)
    def remove(id: PosInt): F[Int]                                    = removef(id)
  }

  def makeDefaultService[F[_]]: RoleService[F] = makeService[F]()

  def withController[F[_]: Effect](
    service: RoleService[F] = makeDefaultService[F]
  )(f: RoleController[F] => F[_]): Unit = {
    (for {
      ctl <- RoleController[F, F](service, Logs.empty[F, F])
      _ <- f(ctl)
    } yield ()).toIO.unsafeRunSync()
  }
}
