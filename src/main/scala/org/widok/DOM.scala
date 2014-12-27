package org.widok

import org.scalajs.dom
import org.scalajs.dom.HTMLElement

import scala.scalajs.js

object DOM {
  def createElement(tagName: String, contents: Seq[View] = Seq.empty): HTMLElement = {
    val elem = dom.document.createElement(tagName)
    contents.foreach(_.render(elem, elem.lastChild))
    // TODO remove cast
    elem.asInstanceOf[HTMLElement]
  }

  def getElement(id: String): Option[HTMLElement] =
    // TODO remove cast
    Option(dom.document.getElementById(id).asInstanceOf[HTMLElement])

  def clear(elem: HTMLElement) {
    while (elem.lastChild != null)
      elem.removeChild(elem.lastChild)
  }

  def elements(name: String, parent: HTMLElement): List[HTMLElement] =
    parent
      .getElementsByTagName(name)
      .asInstanceOf[js.Array[HTMLElement]]
      .toList

  def screenCoordinates(elem: HTMLElement): Position = {
    var pos = Position(elem.offsetLeft, elem.offsetTop)
    var iter = elem

    while (iter.offsetParent != null) {
      val parent = iter.offsetParent.asInstanceOf[HTMLElement]

      pos = Position(
        top = pos.top + parent.offsetLeft,
        left = pos.left + parent.offsetTop)

      if (iter == dom.document.body.firstElementChild)
        return pos

      iter = parent
    }

    pos
  }

  def clientCoordinates(element: HTMLElement) = {
    val boundingClientRect = element.getBoundingClientRect()

    Coordinates(
      width = boundingClientRect.width,
      height = boundingClientRect.height,
      top = boundingClientRect.top + dom.window.pageYOffset,
      left = boundingClientRect.left + dom.window.pageXOffset)
  }

  // Positions an element around a host element. Can be used to implement tooltips.
  def position(element: HTMLElement, hostElement: HTMLElement, placement: Placement) {
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