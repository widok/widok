package org.widok

case class Position(top: Double, left: Double)
case class Coordinates(width: Double, height: Double, top: Double, left: Double)

trait Placement
object Placement {
  object Left extends Placement
  object Right extends Placement
  object Top extends Placement
  object Bottom extends Placement
}

