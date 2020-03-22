import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.apply._
import cats.syntax.functor._
import org.http4s.HttpRoutes
import org.http4s.dsl.impl.Root
import tofu.common.Console
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.server.Router
import org.http4s.server.blaze._
import org.http4s.implicits._

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    Console.apply[IO].putStrLn("Hello, dude") *> {
      val helloWorldService = HttpRoutes.of[IO] {
        case GET -> Root / "hello" / name =>
          Ok(s"Hello, $name.")
      }
      val router = Router("/" -> helloWorldService).orNotFound
      BlazeServerBuilder[IO].bindHttp(8080, "localhost").withHttpApp(router)
        .serve
        .compile
        .drain
        .as(ExitCode.Success)
    }
  }
}
