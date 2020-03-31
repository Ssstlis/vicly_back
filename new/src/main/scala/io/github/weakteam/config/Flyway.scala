package io.github.weakteam.config

import org.flywaydb.core.{Flyway => JFlyway}
import tofu.logging.{Logging, Logs}
import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.effect.Sync

class Flyway[F[_]: Logging: Sync] protected (flyway: JFlyway) {

  def migrate: F[Int] = {
    for {
      _ <- Logging[F].info("Migrate database: start")
      n <- Sync[F].delay(flyway.migrate())
      _ <- Logging[F].info(s"Migrate database: $n migration(s) applied")
    } yield n
  }
}

object Flyway {
  def create[F[_]: Sync](cfg: Config): F[Flyway[F]] = {
    val logsMigration = Logs.sync[F, F]

    for {
      implicit0(logging: Logging[F]) <- logsMigration.byName("migration")
      _ <- logging.info(s"Creating flyway with io.github.weakteam.config: $cfg")

      flyway = JFlyway.configure
        .dataSource(cfg.url, cfg.user, cfg.pass)
        .locations(cfg.locations: _*)
        .load
    } yield new Flyway[F](flyway)
  }

  final case class Config(url: String, user: String, pass: String, locations: Seq[String])
}
