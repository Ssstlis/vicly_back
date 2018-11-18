package utils

import java.math.BigInteger
import java.security.MessageDigest.getInstance

import org.joda.time.DateTime

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
  }
}
