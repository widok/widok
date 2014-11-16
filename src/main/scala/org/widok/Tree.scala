package org.widok

trait Tree[T] {
  val value: Var[T]
  val children: VarBuf[Tree[T]]

  def find(f: Tree[T] => Boolean): Option[Tree[T]] =
    if (f(this)) Some(this)
    else children.elements.foldLeft(Option.empty[Tree[T]]) { (acc, cur) =>
      if (acc.isDefined) acc
      else cur.get.find(f)
    }

  def findChild(f: Var[Tree[T]] => Boolean): Option[Var[Tree[T]]] = {
    val found = children.elements.find(f)
    if (found.isDefined) found
    else children.elements.foldLeft(Option.empty[Var[Tree[T]]]) { (acc, cur) =>
      if (acc.isDefined) acc
      else cur.get.findChild(f)
    }
  }

  def parentOf(node: Tree[T]): Option[Tree[T]] = {
    val found = children.elements.find(
      _.get.children.elements.exists(_.get == node)
    )

    if (found.isDefined) found.map(_.get)
    else children.elements.foldLeft(Option.empty[Tree[T]]) { (acc, cur) =>
      if (acc.isDefined) acc
      else cur.get.parentOf(node)
    }
  }
}

object Tree {
  def apply[T](root: T, child: Tree[T]*): Tree[T] = new Tree[T] {
    val value = Var(root)
    val children = VarBuf(child: _*)
    override def toString = s"Tree($value, $children)"
  }
}