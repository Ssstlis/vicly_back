package models

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class SeaweedResponse(
                            eTag: String,
                            fileId: String,
                            fileName: String,
                            fileUrl: String,
                            fileSize: Int
               ) {
  def toJson = {
    Json.obj(
      "eTag" -> eTag,
      "fileId" -> fileId,
      "fileName" -> fileName,
      "fileUrl" -> fileUrl,
      "fileSize" -> fileSize
    )
  }

}

trait SeaweedResponseJson {

  implicit val writes: Writes[SeaweedResponse] = (u: SeaweedResponse) => {
    u.toJson
  }

  def reads(): Reads[SeaweedResponse] = (
      (__ \ "eTag").read[String] and
      (__ \ "fileId").read[String]  and
      (__ \ "fileName").read[String] and
      (__ \ "fileUrl").read[String] and
      (__ \ "fileSize").read[Int]
    ) (SeaweedResponse.apply _)
}

object SeaweedResponse extends SeaweedResponseJson
