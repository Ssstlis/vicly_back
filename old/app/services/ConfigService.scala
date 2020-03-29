package services

import com.google.inject.{Inject, Singleton}
import pdi.jwt.JwtAlgorithm
import play.api.Configuration

@Singleton
class ConfigService @Inject()(configuration: Configuration) {
  val secret_key = configuration.get[String]("secret.key")
  val message_key = configuration.get[String]("message.key")

  val algo = JwtAlgorithm.HS256
}
