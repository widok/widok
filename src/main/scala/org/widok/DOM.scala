package org.widok

import org.scalajs.dom
import org.scalajs.dom.extensions.PimpedNodeList

object DOM {
  def createElement(tagName: String,
                    contents: Seq[View] = Seq.empty): dom.HTMLElement =
  {
    val elem = dom.document.createElement(tagName)
    contents.foreach(_.render(elem, elem.lastChild))
    // TODO remove cast
    elem.asInstanceOf[dom.HTMLElement]
  }

  def insertAfter(parent: dom.Node, reference: dom.Node, node: dom.Node) {
    if (reference == null || reference.nextSibling == null) parent.appendChild(node)
    else parent.insertBefore(node, reference.nextSibling)
  }

  def getElement(id: String): Option[dom.Element] =
    Option(dom.document.getElementById(id))

  def clear(elem: dom.Node) {
    while (elem.lastChild != null)
      elem.removeChild(elem.lastChild)
  }

  def elements(name: String, parent: dom.Element): List[dom.Node] =
    parent
      .getElementsByTagName(name)
      .toList

  def screenCoordinates(elem: dom.HTMLElement): Position = {
    var pos = Position(elem.offsetLeft, elem.offsetTop)
    var iter = elem

    while (iter.offsetParent != null) {
      val parent = iter.offsetParent.asInstanceOf[dom.HTMLElement]

      pos = Position(
        top = pos.top + parent.offsetLeft,
        left = pos.left + parent.offsetTop)

      if (iter == dom.document.body.firstElementChild)
        return pos

      iter = parent
    }

    pos
  }

  def clientCoordinates(element: dom.HTMLElement) = {
    val boundingClientRect = element.getBoundingClientRect()

    Coordinates(
      width = boundingClientRect.width,
      height = boundingClientRect.height,
      top = boundingClientRect.top + dom.window.pageYOffset,
      left = boundingClientRect.left + dom.window.pageXOffset)
  }

  /** Positions an element around a host element. Can be used to implement tooltips. */
  def position(element: dom.HTMLElement,
               hostElement: dom.HTMLElement,
               placement: Placement)
  {
    val elemPosition = clientCoordinates(element)
    val hostPosition = clientCoordinates(hostElement)

    // Calculate the element's top and left coordinates to center it.
    val position = placement match {
      case Placement.Right =>
        Position(
          top = hostPosition.top + hostPosition.height / 2 - elemPosition.height / 2,
          left = hostPosition.left + hostPosition.width)
      case Placement.Bottom =>
        Position(
          top = hostPosition.top + hostPosition.height,
          left = hostPosition.left + hostPosition.width / 2 - elemPosition.width / 2)
      case Placement.Left =>
        Position(
          top = hostPosition.top + hostPosition.height / 2 - elemPosition.height / 2,
          left = hostPosition.left - elemPosition.width)
      case Placement.Top =>
        Position(
          top = hostPosition.top - elemPosition.height,
          left = hostPosition.left + hostPosition.width / 2 - elemPosition.width / 2)
    }

    element.style.top = s"${position.top}px"
    element.style.left = s"${position.left}px"
    element.style.display = "block"
  }
}