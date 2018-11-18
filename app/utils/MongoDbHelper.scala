package utils

import salat.dao.SalatMongoCursor
import utils.CollectionHelper.TraversableOnceHelper

object MongoDbHelper {

  implicit class MongoDbCursorExtended[T <: AnyRef](c: SalatMongoCursor[T]) {
    def foldHeadO[B](ifEmpty: => B)(f: T => B) = {
      c.limit(1).headOption.fold(ifEmpty)(f)
    }
  }
}
