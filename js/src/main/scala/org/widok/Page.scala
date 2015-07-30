package org.widok

import scala.concurrent.Future

import org.scalajs.dom

trait Page {
  val document = Document

  /** Returns view to be rendered */
  def render(route: InstantiatedRoute): Future[View]

  /** Called when view has been rendered */
  def ready(node: Node) { }

  /** Called upon route change */
  def destroy() { }
}

object PageContainer {
  val node = Node(DOM.getElement("page").orNull.asInstanceOf[dom.html.Element])

  if (node.rendered == null) {
    error("DOM element not found. The JavaScript files must be loaded " +
      "at the end of the HTML document.")
  }

  def replace(view: View) {
    DOM.clear(node.rendered)
    view.render(node.rendered, node.rendered.lastChild)
  }
}
