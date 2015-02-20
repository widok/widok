package org.widok.bindings.Bootstrap

import org.scalajs.dom
import org.scalajs.dom.ext.KeyCode

import org.widok._
import org.widok.html._

/**
 * Ported from https://github.com/bassjobsen/Bootstrap-3-Typeahead/blob/master/bootstrap3-typeahead.js
 */
case class Typeahead[A](
  input: Input.Text
, options: String => Seq[(A, String)]
, select: A => Unit
) extends Widget[Typeahead[A]]
{
  private val shown = Var(false)
  private val value = Var("")
  private val results = Buffer[(A, String)]()
  private val active = Opt[(A, String)]()
  private var handleKeyUp = false

  private val ipt = input
    .autocomplete(false)
    .onKeyDown(onKeyDown)
    .onKeyUp(onKeyUp)

  private val list = ul(
    results.map { case cur @ (id, caption) =>
      li(a(caption).onClick(_ => onSelect(id)))
        .cssState(active.is(cur), "active")
    }
  ).css("typeahead", "dropdown-menu")

  val rendered = span(ipt, list).rendered

  shown.attach {
    case true =>
      val coords = DOM.clientCoordinates(ipt.rendered)
      list.rendered.style.top = (coords.top + coords.height) + "px"
      list.rendered.style.left = s"${coords.left}px"
      list.rendered.style.display = "block"
      list.rendered.style.position = "fixed"
    case false =>
      list.rendered.style.display = "none"
  }

  private def close() {
    shown := false
  }

  private def onSelect(id: A) {
    close()
    select(id)
  }

  private def query(input: String) {
    if (input.nonEmpty && input != value.get) {
      active.clear()
      results.set(options(input))
      shown := results.get.nonEmpty
      value := input
    }
  }

  private def prev() {
    if (results.get.nonEmpty) {
      if (active.isEmpty$) active := results.get.head
      else {
        val before = results.beforeOption$(active.get)
        if (before.isEmpty) active := results.get.last
        else active := before.get
      }
    }
  }

  private def next() {
    if (results.get.nonEmpty) {
      if (active.isEmpty$) active := results.get.head
      else {
        val after = results.afterOption$(active.get)
        if (after.isEmpty) active := results.get.head
        else active := after.get
      }
    }
  }

  private def onKeyDown(e: dom.KeyboardEvent) {
    handleKeyUp = false

    e.keyCode match {
      case KeyCode.enter => onSelect(active.get._1)
      case KeyCode.tab | KeyCode.escape => close()
      case KeyCode.up if !e.shiftKey => // left parenthesis if e.shiftKey
        prev()
      case KeyCode.down if !e.shiftKey => // right parenthesis if e.shiftKey
        next()
      case _ => handleKeyUp = true
    }

    if (!handleKeyUp && e.keyCode != KeyCode.tab) e.preventDefault()
  }

  private def onKeyUp(e: dom.KeyboardEvent) {
    if (handleKeyUp) query(ipt.value.get)
  }
}