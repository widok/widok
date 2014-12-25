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

  // See https://github.com/scala-js/scala-js/issues/1027
  trait Identity {
    private[this] val hash: Int = Identity.nextID()
    override def hashCode(): Int = hash
    override def equals(other: Any) = this.hashCode() == other.hashCode()
  }

  object Identity {
    private[this] var lastID: Int = 0
    private def nextID(): Int = {
      lastID += 1
      lastID
    }
  }
}

/**
 * Reference to a constant value; similar to pointers in low-level languages.
 * To be used when a value is to be identified by its instance as opposed to
 * its contents.
 */
case class Ref[T](get: T) extends Helpers.Identity {
  override def toString = get.toString
}
