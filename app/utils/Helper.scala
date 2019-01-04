package utils

import java.math.BigInteger
import java.security.MessageDigest.getInstance

import org.joda.time.DateTime
import pdi.jwt.JwtJson
import play.api.libs.json.Json
import services.ConfigService

//Helper must help with buildbot triggering_3
object Helper {

  implicit class DateTimeExtended(dt: DateTime) {
    def timestamp = {
      (dt.getMillis / 1000).toInt
    }
  }

  implicit class StringExtended(s: String) {
    def md5: String = {
      f"${new BigInteger(1, getInstance("MD5").digest(s.getBytes("UTF-8")))}%032x"
    }

    def encodeToken(implicit config: ConfigService) = {
      JwtJson.encode(Json.obj("text" -> s), config.message_key, config.algo)
    }

    def decodeToken(implicit config: ConfigService) = {
      JwtJson.decodeJson(s, config.message_key, Seq(config.algo)).toOption
    }
  }
}
