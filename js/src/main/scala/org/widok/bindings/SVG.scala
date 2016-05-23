package org.widok.bindings

import org.scalajs.dom.html.Element
import org.widok.{DOM, View, Widget}

/**
  *
  *
  * @author Matthew Pocock
  */
object SVG {

  implicit val svgNamespace = DOM.Namespace.svg

  case class Svg(contents: View*) extends Widget[Svg] {
    override val rendered: Element = DOM.createElement("svg", contents)
  }

}
