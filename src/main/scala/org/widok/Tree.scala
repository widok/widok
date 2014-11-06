package org.widok

trait Tree[T] {
  val value: Var[T]
  val children: VarBuf[Tree[T]]

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
    val children = VarBuf.unit(child: _*)
    override def toString = "<tree>"
  }
}