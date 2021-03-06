package utils

import java.math.BigInteger
import java.security.MessageDigest.getInstance

import org.joda.time.DateTime
import pdi.jwt.JwtJson
import play.api.libs.json.Json
import services.ConfigService

object Helper {

  implicit class DateTimeExtended(dt: DateTime) {
    def timestamp = {
      (dt.getMillis / 1000).toInt
    }
  }

//  implicit class BooleanConversion(value:Boolean){
//    def Boolean2Result() = {
//      if (value) {
//        Ok
//      } else {
//        BadRequest
//      }
//    }
//  }

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
