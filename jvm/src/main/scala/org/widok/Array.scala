package org.widok

import scala.collection.mutable

/** Wrapper around ArrayBuffer
  * Implements same semantics as its JavaScript counterpart.
  */
case class Array[T]() {
  private val elements = mutable.ArrayBuffer.empty[T]

  def size: Int = elements.length
  def isEmpty: Boolean = elements.length == 0
  def indexOf(value: T): Int = elements.indexOf(value)
  def contains(value: T): Boolean = elements.contains(value)
  def foreach(f: T => Unit) { toList.foreach(f) } /* Allows in-place modifications */
  def clear() { elements.clear() }
  def +=(item: T) { elements += item }
  def -=(item: T) {
    assert(elements.contains(item))
    elements -= item
  }
  def toList: List[T] = List(elements: _*)
}
