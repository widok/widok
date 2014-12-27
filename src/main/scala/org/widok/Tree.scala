package org.widok

trait Tree[T] {
  val value: Var[T]
  val children: Buffer[Tree[T]]

  def find(f: Tree[T] => Boolean): Option[Tree[T]] =
    if (f(this)) Some(this)
    else children.elements.foldLeft(Option.empty[Tree[T]]) { (acc, cur) =>
      if (acc.isDefined) acc
      else cur.get.find(f)
    }

  def findChild(f: Ref[Tree[T]] => Boolean): Option[Ref[Tree[T]]] = {
    val found = children.elements.find(f)
    if (found.isDefined) found
    else children.elements.foldLeft(Option.empty[Ref[Tree[T]]]) { (acc, cur) =>
      if (acc.isDefined) acc
      else cur.get.findChild(f)
    }
  }

  def parentOf(node: Tree[T]): Option[Ref[Tree[T]]] = {
    val found = children.elements.find(
      _.get.children.elements.contains(node)
    )

    if (found.isDefined) found
    else children.elements.foldLeft(Option.empty[Ref[Tree[T]]]) { (acc, cur) =>
      if (acc.isDefined) acc
      else cur.get.parentOf(node)
    }
  }
}

object Tree {
  def apply[T](root: T, child: Tree[T]*): Tree[T] = new Tree[T] {
    val value = Var(root)
    val children = Buffer(child: _*)
    override def toString = s"Tree($value, $children)"
  }
}