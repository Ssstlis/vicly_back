package models

import play.api.libs.json.Json

case class MessageTime(timestamp: Long, zone: Int)

trait MessageTimeJson {
  implicit val writes = Json.writes[MessageTime]
}

object MessageTime extends MessageTimeJson