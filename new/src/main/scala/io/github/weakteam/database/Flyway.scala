package io.github.weakteam.database

import cats.Functor
import cats.effect.Sync
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.github.weakteam.config.AppConfig.DatabaseConfig
import org.flywaydb.core.{Flyway => JFlyway}
import tofu.logging.{Logging, Logs}

class Flyway[F[_]: Sync] protected (flyway: JFlyway)(implicit logger: Logging[F]) {

  def migrate: F[Int] = {
    logger.info("Migrate database: start") *>
      Sync[F].delay(flyway.migrate()).flatTap(n => logger.info(s"Migrate database: $n migration(s) applied"))
  }
}

object Flyway {
  def create[I[_]: Functor, F[_]: Sync](cfg: DatabaseConfig)(implicit logs: Logs[I, F]): I[F[Flyway[F]]] = {
    logs.byName("migration").map { implicit logging =>
      logging.info(s"Creating flyway with io.github.weakteam.config: $cfg").as {
        val flyway = JFlyway.configure
          .dataSource(cfg.url, cfg.user, cfg.password)
          .locations(cfg.locations: _*)
          .load
        new Flyway[F](flyway)
      }
    }
  }
}
