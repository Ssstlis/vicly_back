package config

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.flywaydb.core.{Flyway => JFlyway}

class Flyway[F[_]] protected (flyway: JFlyway, log: Logger[F])(implicit F: Sync[F]) {

  def migrate: F[Int] = {
    for {
      _ <- log.info("Migrate database: start")
      n <- F.delay(flyway.migrate())
      _ <- log.info(s"Migrate database: $n migration(s) applied")
    } yield n
  }
}

object Flyway {
  def create[F[_]: Sync](cfg: Config): F[Flyway[F]] =
    for {
      log <- Slf4jLogger.create[F]
      _ <- log.info(s"Creating flyway with config: $cfg")
      flyway = JFlyway.configure
        .dataSource(cfg.url, cfg.user, cfg.pass)
        .locations(cfg.locations: _*)
        .load
    } yield new Flyway[F](flyway, log)

  final case class Config(url: String, user: String, pass: String, locations: Seq[String])
}
