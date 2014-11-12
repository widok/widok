package org.widok

trait Tree[T] {
  val value: Var[T]
  val children: VarBuf[Tree[T]]

  def find(f: Var[Tree[T]] => Boolean): Option[Var[Tree[T]]] = {
    val elem = children.elements.find(f)
    if (elem.isDefined) elem
    else children.elements.find(
      _.get.find(f).isDefined
    )
  }

  def findChild(child: Var[Tree[T]]): Option[Tree[T]] = {
    if (children.contains(child)) Some(this)
    else children.elements.find(
      _.get.findChild(child).isDefined
    ).map(_.get)
  }
}

object Tree {
  def apply[T](root: T, child: Tree[T]*): Tree[T] = new Tree[T] {
    val value = Var(root)
    val children = VarBuf(child: _*)
    override def toString = "<tree>"
  }
}