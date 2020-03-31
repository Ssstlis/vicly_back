package io.github.weakteam.config

import cats.effect.{Blocker, ContextShift, Sync}
import com.github.ghik.silencer.silent
import io.github.weakteam.config.AppConfig.{DatabaseConfig, HttpConfig}
import pureconfig.generic.auto._
import pureconfig.generic.ProductHint
import pureconfig.module.catseffect.syntax._
import pureconfig.{CamelCase, ConfigFieldMapping, ConfigSource}

final case class AppConfig(http: HttpConfig, db: DatabaseConfig)

object AppConfig {
  def load[F[_]: Sync: ContextShift](blocker: Blocker): F[AppConfig] = {
    @silent implicit def hint[A]: ProductHint[A] = ProductHint(ConfigFieldMapping(CamelCase, CamelCase))
    ConfigSource.default.at("app").loadF(blocker)
  }

  final case class DatabaseConfig(
//      driver: NonEmptyString,
      driver: String,
//      url: NonEmptyString,
      url: String,
//      user: NonEmptyString,
      user: String,
      password: String,
      threadPoolSize: Int,
//      chunks: PosInt
      chunks: Int
  )

  final case class HttpConfig(
//    host: NonEmptyString,
      host: String,
//    port: PosInt
      port: Int
  )
}
