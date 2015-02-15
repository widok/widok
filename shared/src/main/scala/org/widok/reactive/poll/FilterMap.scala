package org.widok.reactive.poll

trait FilterMap[Container[_, _], A, B] {
  /** Only include elements for which `f` is true */
  def filter$(f: ((A, B)) => Boolean): Container[A, B]
}
