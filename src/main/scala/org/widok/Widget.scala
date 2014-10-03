package org.widok

import org.scalajs.dom
import org.scalajs.dom.{MouseEvent, HTMLElement}

import org.widok.bindings._

object Widget {
  def Page(route: InstantiatedRoute)(contents: Widget*) =
    HTML.Anchor(route.uri())(contents: _*)
}

trait Widget {
  val rendered: HTMLElement

  def tag(tagName: String, contents: Widget*) = {
    val elem = dom.document.createElement(tagName)
    contents.foreach(cur => elem.appendChild(cur.rendered))
    elem
  }

  def onClick(f: () => Unit) = {
    this.rendered.onclick = (e: MouseEvent) => f()
    this
  }

  def onDoubleClick(f: () => Unit) = {
    this.rendered.ondblclick = (e: MouseEvent) => f()
    this
  }

  def withId(id: String) = {
    this.rendered.id = id
    this
  }

  def withCursor(cursor: HTML.Cursor) = {
    this.rendered.setAttribute("style", s"cursor: $cursor")
    this
  }

  def withCSS(cssTags: String*) = {
    this.rendered.className = cssTags.mkString(" ")
    this
  }

  def setCSS(cssTag: String, state: Boolean) {
    val tags = this.rendered.className.split(" ").toSet

    val changed =
      if (state) tags + cssTag
      else tags.diff(Set(cssTag))

    this.rendered.className = changed.mkString(" ")
  }

  def withAttribute(key: String, value: String) = {
    this.rendered.setAttribute(key, value)
    this
  }

  def show[T](value: Channel[Boolean]) = {
    value.attach(cur =>
      this.rendered.style.visibility =
        if (cur) "visible"
        else "hidden")
    value.populate()
    this
  }
}
