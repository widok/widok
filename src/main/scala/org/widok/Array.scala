package org.widok

import scala.scalajs.js

/* Wrapper around native JavaScript arrays. */
case class Array[T]() {
  val elements = js.Array[T]()

  def size: Int = elements.length
  def isEmpty: Boolean = elements.length == 0

  /* TODO See also https://github.com/scala-js/scala-js/issues/1447 */
  def indexOf(value: T): Int =
    elements.asInstanceOf[js.Dynamic]
      .indexOf(value.asInstanceOf[js.Dynamic])
      .asInstanceOf[Int]

  def contains(value: T): Boolean = indexOf(value) != -1
  def foreach(f: T => Unit) { elements.toList.foreach(f) }
  def clear() { elements.length = 0 }
  def +=(item: T) { elements.push(item) }
  def -=(item: T) { elements.splice(indexOf(item), 1) }
}
