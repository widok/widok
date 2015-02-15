package org.widok

trait Tree[T] {
  val value: Var[T]
  val children: Buffer[Tree[T]]

  def find(f: Tree[T] => Boolean): Option[Tree[T]] =
    if (f(this)) Some(this)
    else children.elements.foldLeft(Option.empty[Tree[T]]) { (acc, cur) =>
      if (acc.isDefined) acc
      else cur.find(f)
    }

  def findChild(f: Tree[T] => Boolean): Option[Tree[T]] = {
    val found = children.elements.find(f)
    if (found.isDefined) found
    else children.elements.foldLeft(Option.empty[Tree[T]]) { (acc, cur) =>
      if (acc.isDefined) acc
      else cur.findChild(f)
    }
  }

  def parentOf(node: Tree[T]): Option[Tree[T]] = {
    val found = children.elements.find(
      _.children.elements.contains(node)
    )

    if (found.isDefined) found
    else children.elements.foldLeft(Option.empty[Tree[T]]) { (acc, cur) =>
      if (acc.isDefined) acc
      else cur.parentOf(node)
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