package org.widok

trait Disposable {
  def dispose()
}

class Resource[T, U](allocate: T => U, disp: U => Unit) extends Disposable {
  private var res = Option.empty[U]

  def isDefined = res.isDefined

  def apply(): U =
    res.get

  def set(arg: T) {
    if (res.isDefined) disp(res.get)
    res = Some(allocate(arg))
  }

  def dispose() {
    res.foreach(disp)
    res = Option.empty
  }
}

object Resource {
  def apply[T, U](allocate: T => U, disp: U => Unit) = new Resource(allocate, disp)
}
