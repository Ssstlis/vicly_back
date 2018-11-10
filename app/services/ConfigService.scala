package services

import com.google.inject.{Inject, Singleton}
import pdi.jwt.JwtAlgorithm
import play.api.Configuration

@Singleton
class ConfigService @Inject()(configuration: Configuration) {
  val secret_key = "FagP224dddDGGhSxPpjsPabdXJyjE8wx"
  val algo = JwtAlgorithm.HS256
}
