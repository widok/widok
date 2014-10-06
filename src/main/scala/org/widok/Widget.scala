package org.widok

import org.scalajs.dom

import org.widok.bindings._

object Widget {
  def Page(route: InstantiatedRoute)(contents: Widget*) =
    HTML.Anchor(route.uri())(contents: _*)
}

object Event {
  trait Mouse
  object Mouse {
    case object Click extends Mouse
    case object DoubleClick extends Mouse
    case object Leave extends Mouse
    case object Enter extends Mouse
    case object Out extends Mouse
    case object Up extends Mouse
    case object Over extends Mouse
    case object Down extends Mouse
    case object Move extends Mouse
    case object ContextMenu extends Mouse
  }

  trait Key
  object Key {
    case object Down extends Key
    case object Up extends Key
    case object Press extends Key
  }
}

trait Widget {
  val rendered: dom.HTMLElement

  def tag(tagName: String, contents: Widget*) = {
    val elem = dom.document.createElement(tagName)
    contents.foreach(cur => elem.appendChild(cur.rendered))
    elem
  }

  def bindMouse(event: Event.Mouse, writeChannel: Channel[dom.MouseEvent]) = {
    import Event.Mouse._
    event match {
      case Click => this.rendered.onclick = (e: dom.MouseEvent) => writeChannel.produce(e)
      case DoubleClick => this.rendered.ondblclick = (e: dom.MouseEvent) => writeChannel.produce(e)
      case Leave => this.rendered.onmouseleave = (e: dom.MouseEvent) => writeChannel.produce(e)
      case Enter => this.rendered.onmouseenter = (e: dom.MouseEvent) => writeChannel.produce(e)
      case Out => this.rendered.onmouseout = (e: dom.MouseEvent) => writeChannel.produce(e)
      case Up => this.rendered.onmouseup = (e: dom.MouseEvent) => writeChannel.produce(e)
      case Over => this.rendered.onmouseover = (e: dom.MouseEvent) => writeChannel.produce(e)
      case Down => this.rendered.onmousedown = (e: dom.MouseEvent) => writeChannel.produce(e)
      case Move => this.rendered.onmousemove = (e: dom.MouseEvent) => writeChannel.produce(e)
      case ContextMenu => this.rendered.oncontextmenu = (e: dom.MouseEvent) => writeChannel.produce(e)
    }

    this
  }

  def bindKey(event: Event.Key, writeChannel: Channel[dom.KeyboardEvent]) = {
    import Event.Key._
    event match {
      case Up => this.rendered.onkeyup = (e: dom.KeyboardEvent) => writeChannel.produce(e)
      case Down => this.rendered.onkeydown = (e: dom.KeyboardEvent) => writeChannel.produce(e)
      case Press => this.rendered.onkeypress = (e: dom.KeyboardEvent) => writeChannel.produce(e)
    }

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

  def withCSS(state: Channel[Boolean], cssTags: String*) = {
    state.attach(value => cssTags.foreach(cssTag => setCSS(cssTag, value)))
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

  def show[T](value: Channel[Boolean], remove: Boolean = true) = {
    value.attach(cur =>
      if (remove) {
        this.rendered.style.display =
          if (cur) "block" else "none"
      } else {
        this.rendered.style.visibility =
          if (cur) "visible" else "hidden"
      })

    value.populate()
    this
  }
}
