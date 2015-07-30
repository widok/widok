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
