package org.widok

import org.scalajs.dom

import org.widok.bindings._

import scala.collection.mutable

object Widget {
  object List {
    trait Item extends Widget
  }

  trait List extends Widget {
    def bind[T, U <: Seq[T]](channel: Channel[U])(f: T => List.Item) = {
      channel.attach(list => {
        DOM.clear(rendered)

        list.foreach { cur =>
          rendered.appendChild(f(cur).rendered)
        }
      })

      this
    }

    def bind[T](aggregate: Aggregate[T])(f: Channel[T] => List.Item) = {
      var map = mutable.Map[Channel[T], Widget]()

      aggregate.attach(new Aggregate.Observer[T] {
        def append(cur: Channel[T]) {
          val li = f(cur)
          rendered.appendChild(li.rendered)
          map += (cur -> li)
        }

        def remove(cur: Channel[T]) {
          rendered.removeChild(map(cur).rendered)
          map -= cur
        }
      })

      this
    }
  }

  trait Container extends Widget {
    def bindString[T <: String](value: Channel[T]) = {
      value.attach(cur => rendered.textContent = cur.toString)
      this
    }

    def bindInt[T <: Int](value: Channel[T]) = {
      value.attach(cur => rendered.textContent = cur.toString)
      this
    }

    def bindDouble[T <: Double](value: Channel[T]) = {
      value.attach(cur => rendered.textContent = cur.toString)
      this
    }

    def bindBoolean[T <: Boolean](value: Channel[T]) = {
      value.attach(cur => rendered.textContent = cur.toString)
      this
    }

    def bindWidget[T <: Widget](value: Channel[T]) = {
      value.attach(cur => {
        if (rendered.firstChild != null) rendered.removeChild(rendered.firstChild)
        rendered.appendChild(cur.rendered)
      })

      this
    }

    def bindOptWidget[T <: Option[Widget]](value: Channel[T]) = {
      value.attach(cur => {
        if (rendered.firstChild != null) rendered.removeChild(rendered.firstChild)
        if (cur.isDefined) rendered.appendChild(cur.get.rendered)
      })

      this
    }

    // Bind HTML.
    def bindRaw[T](value: Channel[String]) = {
      value.attach(cur => {
        rendered.innerHTML = cur
      })

      this
    }
  }
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

  trait Touch
  object Touch {
    case object Start extends Touch
    case object Move extends Touch
    case object End extends Touch
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

  def bindTouch(event: Event.Touch, writeChannel: Channel[dom.TouchEvent]) = {
    import Event.Touch._
    val ev = event match {
      case Start => "ontouchstart"
      case Move => "ontouchmove"
      case End => "ontouchend"
    }

    this.rendered.addEventListener(
      ev,
      (e: dom.Event) => writeChannel.produce(e.asInstanceOf[dom.TouchEvent]),
      useCapture = false)

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
    val tags = this.rendered.className.split(" ").toSet
    this.rendered.className = (tags ++ cssTags).mkString(" ")
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

    this
  }
}
