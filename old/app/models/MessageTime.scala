package models

import java.util.Calendar

import org.joda.time.DateTime
import play.api.libs.json.Json

case class MessageTime(
  timestamp: Long = DateTime.now.getMillis,
  zone: String = s"UTC+${Calendar.getInstance().getTimeZone.getRawOffset / 3600000}"
)

trait MessageTimeJson {
  implicit val writes = Json.writes[MessageTime]
}

object MessageTime extends MessageTimeJson