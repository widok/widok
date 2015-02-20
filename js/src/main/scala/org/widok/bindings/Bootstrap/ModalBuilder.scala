package org.widok.bindings.Bootstrap

import org.scalajs.dom

import org.widok._

case class ModalBuilder(contents: Modal.ContentElement*) extends Widget[ModalBuilder] {
  val shown = Var(false)
  val height = Var("")

  def open() = { shown := true; this }
  def dismiss() = { shown := false; this }

  val rendered = Modal(
    Modal.Backdrop().attribute("style", height)
    , Modal.Dialog(
      Modal.Content(contents: _*)
    )
  ).fade(true)
    .cssState(shown, "in")
    .rendered

  /* .show(shown) wouldn't work here because Bootstrap uses
   * `style.display = none` in its stylesheet.
   */

  val resize = (e: dom.Event) => {
    val h = dom.document.body.scrollHeight
    height := s"height: ${h}px"
  }

  shown.attach(
    if (_) {
      Document.body.className += "modal-open"
      style.display := "block"
      dom.window.addEventListener("resize", resize)
      resize(null) /* Set initial height */
    } else {
      Document.body.className -= "modal-open"
      style.display := "none"
      dom.window.removeEventListener("resize", resize)
    }
  )
}
