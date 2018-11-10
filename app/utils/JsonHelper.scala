package utils

import org.bson.types.ObjectId
import play.api.libs.json._

object JsonHelper {

  implicit val ObjectIdFormat: Format[ObjectId] = new Format[ObjectId] {

    def reads(json: JsValue): JsResult[ObjectId] = json match {
      case jsString: JsString if ObjectId.isValid(jsString.value) => JsSuccess(new ObjectId(jsString.value))
      case _ => JsError("error.expected.ObjectId.valid")
    }

    def writes(oId: ObjectId): JsValue = {
      JsString(oId.toString)
    }

  }
}
