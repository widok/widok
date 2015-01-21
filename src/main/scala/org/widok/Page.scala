package org.widok

import org.scalajs.dom

trait BasePage {
  val node = Node(DOM.getElement("page").orNull.asInstanceOf[dom.HTMLElement])

  def view(): View

  def render() {
    node.rendered match {
      case null =>
        sys.error("DOM element not found. The JavaScript files must be loaded " +
          "at the end of the HTML document.")
      case page =>
        DOM.clear(page)
        view().render(page, page.lastChild)
    }
  }

  def destroy() { }
}

trait Page extends BasePage {
  def ready(route: InstantiatedRoute)

  def render(route: InstantiatedRoute) {
    render()
    ready(route)
  }
}
