package org.widok

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExportDescendentObjects, JSExport}

@JSExportDescendentObjects
class Application(enabled: Set[Route], fallback: Route) extends js.JSApp {
  @JSExport
  def main() {
    val router = Router(enabled, fallback = Some(fallback))
    router.listen()
  }
}
