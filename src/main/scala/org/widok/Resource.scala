package org.widok

trait Disposable {
  def dispose()
}

class Resource[T](allocate: => T, disp: T => Unit) extends Disposable {
  var res = Option.empty[T]

  def apply(): T = {
    if (res.isEmpty) res = Some(allocate)
    res.get
  }

  def dispose() {
    res.foreach(disp)
    res = Option.empty
  }
}

object Resource {
  def apply[T](allocate: => T, disp: T => Unit) = new Resource(allocate, disp)
}
