package config

import cats.effect.{Async, ContextShift}
import cats.implicits._
import ciris._
import ciris.refined._
import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.types.string.NonEmptyString

final case class AppConfig(http: HttpConfig, db: DatabaseConfig)

final case class HttpConfig(host: NonEmptyString, port: PosInt)

case class DatabaseConfig(
    driver: NonEmptyString,
    url: NonEmptyString,
    user: NonEmptyString,
    password: NonEmptyString,
    threadPoolSize: PosInt
)

object AppConfig {
  def loadConfig[F[_]: Async: ContextShift]: F[AppConfig] = {
    (httpDecoder, databaseDecoder).mapN(AppConfig.apply).load[F]
  }

  private val httpDecoder: ConfigValue[HttpConfig] = {
    (
      prop("http.host").as[NonEmptyString],
      prop("http.port").as[PosInt]
    ).mapN(HttpConfig.apply)
  }

  private val databaseDecoder: ConfigValue[DatabaseConfig] = {
    (
      prop("database.driver").as[NonEmptyString],
      prop("database.url").as[NonEmptyString],
      prop("database.user").as[NonEmptyString],
      prop("database.password").as[NonEmptyString],
      prop("database.thread").as[PosInt]
    ).mapN(DatabaseConfig.apply)
  }
}
