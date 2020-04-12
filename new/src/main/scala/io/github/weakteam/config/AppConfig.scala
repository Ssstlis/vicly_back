package io.github.weakteam.config

import cats.data.NonEmptyList
import cats.effect.{Blocker, ContextShift, Sync}
import com.github.ghik.silencer.silent
import eu.timepit.refined.pureconfig._
import eu.timepit.refined.types.numeric.{PosInt, PosLong}
import eu.timepit.refined.types.string.NonEmptyString
import io.github.weakteam.config.AppConfig.{Cors, DatabaseConfig, GZip, HttpConfig}
import org.http4s.server.middleware.CORSConfig
import pureconfig.generic.auto._
import pureconfig.generic.ProductHint
import pureconfig.module.catseffect.syntax._
import pureconfig.module.cats.nonEmptyListReader
import pureconfig.{CamelCase, ConfigFieldMapping, ConfigSource}

final case class AppConfig(http: HttpConfig, db: DatabaseConfig, cors: Cors, gzip: GZip)

object AppConfig {

  def load[F[_]: Sync: ContextShift](blocker: Blocker): F[AppConfig] = {
    @silent implicit def hint[A]: ProductHint[A] = ProductHint(ConfigFieldMapping(CamelCase, CamelCase))
    ConfigSource.default.at("app").loadF(blocker)
  }

  final case class DatabaseConfig(
    driver: NonEmptyString,
    url: NonEmptyString,
    user: NonEmptyString,
    password: String,
    threadPoolSize: PosInt,
    chunks: PosInt,
    maxLifetime: Option[PosLong],
    connTimeout: Option[PosLong],
    locations: NonEmptyList[NonEmptyString]
  )

  final case class HttpConfig(
    host: NonEmptyString,
    port: PosInt
  )

  final case class Cors(
    allowedOrigins: NonEmptyList[String],
    allowedMethods: NonEmptyList[String],
    allowedHeaders: NonEmptyList[String],
    maxAge: Long = 0L,
    exposedHeaders: Set[String] = Set.empty[String]
  ) {
    def toHttp4sCors: CORSConfig = {
      val originsSet = allowedOrigins.toList.toSet
      CORSConfig(
        allowCredentials = true,
        anyOrigin = false,
        allowedOrigins = origin => originsSet.contains(origin) || originsSet.contains("*"),
        anyMethod = false,
        allowedMethods = Some(allowedMethods.toList.toSet),
        allowedHeaders = Some(allowedHeaders.toList.toSet),
        exposedHeaders = Some(exposedHeaders),
        maxAge = maxAge
      )
    }
  }

  final case class GZip(bufferSizeMultiplier: PosInt)
}
