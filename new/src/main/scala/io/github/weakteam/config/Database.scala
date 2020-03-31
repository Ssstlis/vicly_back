package io.github.weakteam.config

import cats.effect.{Async, Blocker, ContextShift, Resource}
import io.github.weakteam.config.AppConfig.DatabaseConfig
import doobie.hikari.HikariTransactor

import scala.concurrent.ExecutionContext

object Database {
  def transactor[F[_]: Async: ContextShift](
      config: DatabaseConfig,
      executionContext: ExecutionContext,
      blocker: Blocker
  ): Resource[F, HikariTransactor[F]] = {
    HikariTransactor.newHikariTransactor[F](
      config.driver,
      config.url,
      config.user,
      config.password,
      executionContext,
      blocker
    )
  }
}
