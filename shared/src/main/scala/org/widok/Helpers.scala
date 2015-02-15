package org.widok

object Helpers {
  def after(haystack: String, needle: Char) = {
    val index = haystack.indexOf(needle)
    if (index != -1) Some(haystack.substring(index + 1))
    else None
  }

  // From http://stackoverflow.com/questions/10160280/how-to-implement-generic-average-function-in-scala
  def average[T](ts: Iterable[T])(implicit num: Numeric[T]) = {
    if (ts.size == 0) None
    else Some(num.toDouble(ts.sum) / ts.size)
  }

  implicit class IterableWithAvg[T: Numeric](data: Iterable[T]) {
    def avg = average(data)
  }
}

/**
 * Ref makes references to values explicit. In Scala, objects may have different
 * equality semantics. For example, case classes always implement structural
 * equality, but ordinary classes not necessarily. To use different instances of
 * the same value in a hash table, all objects must be wrapped. Ref is a simple
 * solution for this and ensures that physical equality is always performed as
 * hashCode cannot be overridden.
 */
sealed class Ref[T](val get: T) {
  override def toString = get.toString
}

object Ref {
  def apply[T](get: T) = new Ref[T](get)
  def unapply[T](ref: Ref[T]): Option[T] = Some(ref.get)
}
