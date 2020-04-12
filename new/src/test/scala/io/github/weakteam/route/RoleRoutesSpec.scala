package io.github.weakteam.route

import cats.effect.{Effect, IO}
import cats.effect.syntax.effect._
import cats.syntax.applicative._
import cats.syntax.apply._
import io.github.weakteam.controller.RoleController
import org.http4s.{HttpRoutes, Method, Request, Response, Status}
import org.http4s.dsl.Http4sDsl
import org.scalatest.matchers.must
import org.scalatest.wordspec.AnyWordSpec
import RoleRoutesSpec._
import cats.Applicative
import com.github.ghik.silencer.silent
import org.http4s.syntax.literals._

class RoleRoutesSpec extends AnyWordSpec with must.Matchers {

  "RoleRotes#Router" should {

    "return Not Found" in withRoutes[IO]() { router =>
      router
        .apply(Request(uri = uri"/all"))
        .value
        .map(_ mustBe None)
    }
  }

  "RoleRoutes#[GET /role/list]" should {

    "return Ok" in withRoutes[IO](makeController(listf = _ => Response[IO](status = Status.Ok).pure[IO])) { router =>
      router
        .apply(Request(uri = uri"/role/list"))
        .value
        .map(_.get.status mustBe Status.Ok)
    }
  }

  "RoleRoutes#[GET /role/id" should {
    "return Ok (flaky)" in withRoutes[IO](
      makeController(onef = { id =>
        id mustBe "flaky"
        Response[IO](status = Status.Ok).pure[IO]
      })
    ) { router =>
      router
        .apply(Request(uri = uri"/role/flaky"))
        .value
        .map(_.get.status mustBe Status.Ok)
    }
  }

  "RoleRoutes#[POST /role" should {
    "return Ok" in withRoutes[IO](
      makeController(addf = { req =>
        req.attemptAs[String].value.map(_ mustBe Right("test")) *>
          Response[IO](status = Status.Ok).pure[IO]
      })
    ) { router =>
      router
        .apply(Request(uri = uri"/role", method = Method.POST).withEntity("test"))
        .value
        .map(_.get.status mustBe Status.Ok)
    }
  }

  "RoleRoutes#[PATCH /role/id" should {
    "return Ok" in withRoutes[IO](
      makeController(updatef = { (req, id) =>
        id mustBe "flaky"
        req.attemptAs[String].value.map(_ mustBe Right("test")) *>
          Response[IO](status = Status.Ok).pure[IO]
      })
    ) { router =>
      router
        .apply(Request(uri = uri"/role/flaky", method = Method.PATCH).withEntity("test"))
        .value
        .map(_.get.status mustBe Status.Ok)
    }
  }

  "RoleRoutes#[DELETE /role/id" should {
    "return Ok" in withRoutes[IO](
      makeController(deletef = { id =>
        id mustBe "flaky"
        Response[IO](status = Status.Ok).pure[IO]
      })
    ) { router =>
      router
        .apply(Request(uri = uri"/role/flaky", method = Method.DELETE))
        .value
        .map(_.get.status mustBe Status.Ok)
    }
  }
}

object RoleRoutesSpec {

  def defaultResponse[F[_]]: Response[F] = Response[F](status = Status(444))

  def makeController[F[_]: Applicative](
    onef: String => F[Response[F]] = null,
    listf: Request[F] => F[Response[F]] = null,
    addf: Request[F] => F[Response[F]] = null,
    deletef: String => F[Response[F]] = null,
    updatef: (Request[F], String) => F[Response[F]] = null
  ): RoleController[F] = new RoleController[F] {
    def one(id: String): F[Response[F]] =
      if (onef == null) defaultResponse[F].pure[F] else onef(id)

    def list(request: Request[F]): F[Response[F]] =
      if (listf == null) defaultResponse[F].pure[F] else listf(request)

    def add(request: Request[F]): F[Response[F]] =
      if (addf == null) defaultResponse[F].pure[F] else addf(request)

    def delete(id: String): F[Response[F]] =
      if (deletef == null) defaultResponse[F].pure[F] else deletef(id)

    def update(request: Request[F], id: String): F[Response[F]] =
      if (updatef == null) defaultResponse[F].pure[F] else updatef(request, id)
  }

  def makeDefaultController[F[_]: Applicative]: RoleController[F] = makeController[F]()

  @silent
  def withRoutes[F[_]: Effect](ctl: RoleController[F] = null)(f: HttpRoutes[F] => F[_]): Unit = {
    implicit val dsl: Http4sDsl[F] = new Http4sDsl[F] {}
    f(RoleRoutes.routes[F](if (ctl == null) makeDefaultController[F] else ctl)).toIO.unsafeRunSync()
  }
}
