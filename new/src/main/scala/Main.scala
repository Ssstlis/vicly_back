import cats.Applicative
import cats.effect.{ConcurrentEffect, ExitCode, IO, IOApp, Sync, Timer}
import cats.syntax.apply._
import cats.syntax.functor._
import io.circe.Json
import org.http4s.Status
import org.http4s.{EntityEncoder, HttpRoutes, Response}
import org.http4s.dsl.impl.Root
import tofu.common.Console
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.server.Router
import org.http4s.server.blaze._
import org.http4s.implicits._

object Main extends IOApp {

  final class StatusPartialApplied[F[_]](status: Status) {
    def apply[A: EntityEncoder[F, *]](body: A)(implicit F: Applicative[F]): F[Response[F]] = {
      F.pure(Response[F](status = status, body = EntityEncoder[F, A].toEntity(body).body))
    }
  }

  def Ok[F[_]] = new StatusPartialApplied[F](Status.Ok)

  def BadRequest[F[_]] = new StatusPartialApplied[F](Status.BadRequest)

  def mkRoutes[F[_]](implicit F: Sync[F]): HttpRoutes[F] = {
    val helloRoutes = HttpRoutes.of[F] {
      case GET -> Root / "hello" / name =>
        Ok[F](s"Hello, $name.")
      case a @ POST -> Root =>
        a.attemptAs[Json].foldF(
          failure => BadRequest[F](failure.toString),
          Ok[F](_)
        )
    }

    Router("/" -> helloRoutes)
  }

  def mkApp[F[_]: ConcurrentEffect: Timer]: F[ExitCode] = {

    val router = mkRoutes[F].orNotFound
    BlazeServerBuilder[F]
      .bindHttp(8080, "localhost")
      .withHttpApp(router)
      .withNio2(true)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
  }


  override def run(args: List[String]): IO[ExitCode] = {
    Console.apply[IO].putStrLn("Hello, dude") *> mkApp[IO]
  }
}
