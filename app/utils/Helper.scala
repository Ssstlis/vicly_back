package utils

import org.joda.time.DateTime

object Helper {

  implicit class DateTimeExtended(dt: DateTime) {
    def timestamp = {
      (dt.getMillis / 1000).toInt
    }
  }
}
