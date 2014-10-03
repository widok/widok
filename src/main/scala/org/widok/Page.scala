package org.widok

trait Page {
  def contents(): Seq[Widget]

  def ready(route: InstantiatedRoute)

  def render(route: InstantiatedRoute) {
    (for {
      page <- DOM.element("page")
    } yield {
      DOM.clear(page)

      contents().foreach(elem =>
        page.appendChild(elem.rendered))

      this.ready(route)
    }).orElse {
      sys.error("DOM element not found. The JavaScript files must be loaded at the end of the HTML document.")
    }
  }
}
