package org.widok

trait BasePage {
  def view(): View

  def render() {
    (for {
      page <- DOM.getElement("page")
    } yield {
      DOM.clear(page)
      view().render(page, page.lastChild)
    }).orElse {
      sys.error("DOM element not found. The JavaScript files must be loaded at the end of the HTML document.")
    }
  }
}

trait Page extends BasePage {
  def ready(route: InstantiatedRoute)

  def render(route: InstantiatedRoute) {
    render()
    ready(route)
  }
}
