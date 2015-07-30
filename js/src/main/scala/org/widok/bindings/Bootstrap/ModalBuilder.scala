package org.widok.bindings.Bootstrap

import org.scalajs.dom

import pl.metastack.metarx._

import org.widok._

case class ModalBuilder(contents: Modal.ContentElement*)
  extends Widget[ModalBuilder]
  with Disposable
{
  val shown  = Var(false)
  val height = Opt[Length]()

  def open()    = { shown := true ; this }
  def dismiss() = { shown := false; this }

  val backdrop = Modal.Backdrop()

  // TODO Backported to v3.3.2
  // From https://github.com/twbs/bootstrap/commit/f5beebe726aa8c1810015d8c62931f4559b49664
  backdrop.rendered.style.bottom = "0"
  backdrop.rendered.style.position = "fixed"

  val modal = Modal(
    Modal.Dialog(
      Modal.Content(contents: _*)
    )
  ).fade(true)
   .cssState(shown, "in")

  val rendered = modal.rendered

  /* .show(shown) wouldn't work here because Bootstrap uses
   * `style.display = none` in its stylesheet.
   */
  modal.style.display << shown.map(shown => Some(if (shown) "block" else "none"))

  val ch = shown.tail.distinct.attach(
    if (_) {
      dom.document.body.appendChild(backdrop.rendered)
      Document.body.className += "modal-open"
    } else {
      dom.document.body.removeChild(backdrop.rendered)
      Document.body.className -= "modal-open"
    }
  )

  def dispose() {
    ch.dispose()
  }
}