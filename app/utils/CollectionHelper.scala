package utils

object CollectionHelper {

  implicit class TraversableOnceHelper[T, B](t: TraversableOnce[T]) {
    def zipBy(f: T => B): Map[B, T] = {
      t.map(p => f(p) -> p).toMap
    }
  }

}
