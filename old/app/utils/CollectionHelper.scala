package utils

object CollectionHelper {

  implicit class TraversableOnceHelper[+T](t: TraversableOnce[T]) {
    def zipBy[B](f: T => B): Map[B, T] = {
      t.map(p => f(p) -> p).toMap
    }

    def headOption = {
      t.find(_ => true)
    }
  }
}
