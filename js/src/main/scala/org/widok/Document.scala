package org.widok

import org.scalajs.dom

object Document {
  lazy val click = DOMChannel.mouseEvent(dom.document.onclick = _)
  lazy val doubleClick = DOMChannel.mouseEvent(dom.document.ondblclick = _)
  lazy val mouseOut = DOMChannel.mouseEvent(dom.document.onmouseout = _)
  lazy val mouseUp = DOMChannel.mouseEvent(dom.document.onmouseup = _)
  lazy val mouseOver = DOMChannel.mouseEvent(dom.document.onmouseover = _)
  lazy val mouseDown = DOMChannel.mouseEvent(dom.document.onmousedown = _)
  lazy val mouseMove = DOMChannel.mouseEvent(dom.document.onmousemove = _)
  lazy val contextMenu = DOMChannel.mouseEvent(dom.document.oncontextmenu = _)

  lazy val keyUp = DOMChannel.keyboardEvent(dom.document.onkeyup = _)
  lazy val keyDown = DOMChannel.keyboardEvent(dom.document.onkeydown = _)
  lazy val keyPress = DOMChannel.keyboardEvent(dom.document.onkeypress = _)

  lazy val change = DOMChannel.event(dom.document.onchange = _)

  val body = Node(dom.document.body)
}
