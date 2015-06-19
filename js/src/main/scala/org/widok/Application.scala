package org.widok

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExportDescendentObjects, JSExport}

@JSExportDescendentObjects
trait Application extends js.JSApp {
  @JSExport
  def main()
}

class RoutingApplication(enabled: Set[Route], fallback: Route) extends Application {
  def main() {
    val router = Router(enabled, fallback = Some(fallback))
    router.listen()
  }
}

trait PageApplication extends Application {
  def view(): View
  def ready()

  def main() {
    PageContainer.replace(view())
    ready()
  }
}
