package config

import cats.effect.{Async, Blocker, ContextShift, Resource, Sync}
import doobie.hikari.HikariTransactor
import org.flywaydb.core.Flyway

import scala.concurrent.ExecutionContext

object Database {
  def transactor[F[_]: Async: ContextShift](
      config: DatabaseConfig,
      executionContext: ExecutionContext,
      blocker: Blocker
  ): Resource[F, HikariTransactor[F]] = {
    HikariTransactor.newHikariTransactor[F](
      config.driver.value,
      config.url.value,
      config.user.value,
      config.password.value,
      executionContext,
      blocker
    )
  }

  def initialize[F[_]: Sync](transactor: HikariTransactor[F]): F[Unit] = {
    transactor.configure { dataSource =>
      Sync[F].delay {
        val flyWay = Flyway.configure().dataSource(dataSource).load()
        flyWay.migrate()
        ()
      }
    }
  }
}
