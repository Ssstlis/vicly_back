package config

import cats.effect.{Async, Blocker, ContextShift, Resource}
import doobie.hikari.HikariTransactor

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
      config.password.valueHash,
      executionContext,
      blocker
    )
  }
}
