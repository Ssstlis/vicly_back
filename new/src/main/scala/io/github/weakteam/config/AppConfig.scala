package io.github.weakteam.config

import cats.effect.{Blocker, ContextShift, Sync}
import com.github.ghik.silencer.silent
import io.github.weakteam.config.AppConfig.{Cors, DatabaseConfig, GZip, HttpConfig}
import org.http4s.server.middleware.CORSConfig
import pureconfig.generic.auto._
import pureconfig.generic.ProductHint
import pureconfig.module.catseffect.syntax._
import pureconfig.{CamelCase, ConfigFieldMapping, ConfigSource}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._

final case class AppConfig(http: HttpConfig, db: DatabaseConfig, cors: Cors, gzip: GZip)

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
    chunks: Int,
    maxLifetime: Option[Long],
    connTimeout: Option[Long],
    locations: Seq[String]
  )

  final case class HttpConfig(
//    host: NonEmptyString,
    host: String,
//    port: PosInt
    port: Int
  )

  final case class Cors(
    allowedOrigins: Set[String],
    allowedMethods: Set[String],
    allowedHeaders: Set[String],
    maxAge: FiniteDuration = 30.minutes,
    exposedHeaders: Set[String] = Set.empty[String]
  ) {
    def toHttp4sCors: CORSConfig = {
      CORSConfig(
        allowCredentials = true,
        anyOrigin = false,
        allowedOrigins = origin => allowedOrigins.contains(origin) || allowedOrigins.contains("*"),
        anyMethod = false,
        allowedMethods = Some(allowedMethods),
        allowedHeaders = Some(allowedHeaders),
        exposedHeaders = Some(exposedHeaders),
        maxAge = maxAge.toSeconds
      )
    }
  }

  final case class GZip(bufferSizeMultiplier: Int)
}
