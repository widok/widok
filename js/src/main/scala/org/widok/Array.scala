package org.widok

import scala.scalajs.js

/* Wrapper around native JavaScript arrays. */
case class Array[T]() {
  val elements = js.Array[T]()

  def size: Int = elements.length
  def isEmpty: Boolean = elements.length == 0
  def indexOf(value: T): Int = elements.indexOf(value)
  def contains(value: T): Boolean = elements.indexOf(value) != -1
  def foreach(f: T => Unit) { elements.toList.foreach(f) }
  def clear() { elements.length = 0 }
  def +=(item: T) { elements.push(item) }
  def -=(item: T) {
    val idx = elements.indexOf(item)
    assert(idx != -1)
    elements.splice(idx, 1)
  }
  def toList: List[T] = elements.toList
}
