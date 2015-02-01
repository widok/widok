package org.widok

import org.scalajs.dom
import org.scalajs.dom.extensions.KeyCode

import org.widok.bindings._

import scala.collection.mutable
import scala.scalajs.js

object Widget {
  object List {
    trait Item[V] extends Widget[V] { self: V =>

    }
  }

  trait List[V, ListItem <: Widget[_]] extends Widget[V] { self: V =>
    def subscribe[T](channel: ReadChannel[Seq[T]])(f: T => List.Item[_]) = {
      channel.attach { list =>
        DOM.clear(rendered)

        list.foreach { cur =>
          rendered.appendChild(f(cur).rendered)
        }
      }

      self
    }

    def bind[T](aggregate: Aggregate[T])(f: Ref[T] => ListItem) = {
      import Aggregate.Change
      import Aggregate.Position

      val mapping = mutable.Map.empty[Ref[T], dom.Node]

      aggregate.changes.attach {
        case Change.Insert(Position.Head(), element) =>
          mapping += element -> rendered.insertBefore(f(element).rendered, rendered.firstChild)

        case Change.Insert(Position.Last(), element) =>
          mapping += element -> rendered.appendChild(f(element).rendered)

        case Change.Insert(Position.Before(reference), element) =>
          mapping += element -> rendered.insertBefore(f(element).rendered, mapping(reference))

        case Change.Insert(Position.After(reference), element) =>
          val after = f(element).rendered
          DOM.insertAfter(rendered, mapping(reference), after)
          mapping += element -> after

        case Change.Update(reference, element) =>
          mapping += element -> rendered.replaceChild(f(element).rendered, mapping(reference))
          mapping -= reference

        case Change.Remove(element) =>
          rendered.removeChild(mapping(element))
          mapping -= element

        case Change.Clear() =>
          mapping.clear()
          DOM.clear(rendered)
      }

      self
    }
  }

  object Input {
    trait Text[V] extends Widget[V] { self: V =>
      val rendered: dom.HTMLInputElement

      /** Produce current value after enter was pressed. */
      lazy val value = PtrVar[String](
        keyUp.filter(_.keyCode == KeyCode.enter),
        rendered.value, rendered.value = _)

      /** Produce current value after every key press. */
      lazy val liveValue = PtrVar[String](
        keyUp, rendered.value, rendered.value = _)

      def bind(ch: Channel[String]) = { value.bind(ch); self }
      def attach(f: String => Unit) = { value.attach(f); self }

      def bindLive(ch: Channel[String]) = { liveValue.bind(ch); self }
      def attachLive(f: String => Unit) = { liveValue.attach(f); self }
    }

    trait Checkbox[V] extends Widget[V] { self: V =>
      val rendered: dom.HTMLInputElement

      lazy val checked = PtrVar[Boolean](change,
        rendered.checked, rendered.checked = _)

      def bind(ch: Channel[Boolean]) = { checked.bind(ch); self }
      def attach(f: Boolean => Unit) = { checked.attach(f); self }
    }

    trait Select[V] extends Widget[V] { self: V =>
      private var optDefault = Option.empty[HTML.Input.Select.Option]

      private def addOption(str: String): HTML.Input.Select.Option = {
        val elem = HTML.Input.Select.Option()
        elem.rendered.appendChild(HTML.Text(str).rendered)
        elem
      }

      /* All manually added elements will be ignored by bind() */
      def default(caption: String) = {
        assert(optDefault.isEmpty, "default() can only be used once")
        assert(!rendered.hasChildNodes(), "DOM modified externally")
        val opt = addOption(caption)
        rendered.appendChild(opt.rendered)
        optDefault = Some(opt)
        self
      }

      def bind[T, X <: List.Item[X]](map: ChildMap[T, String], selection: Channel[Option[Ref[T]]]) = {
        import Aggregate.Change
        import Aggregate.Position

        def selected(): dom.HTMLSelectElement = {
          val castRendered = rendered.asInstanceOf[dom.HTMLSelectElement]
          val idx = castRendered.selectedIndex

          // TODO Remove casts
          // See https://github.com/scala-js/scala-js/issues/1384#issuecomment-66908750
          // and https://github.com/scala-js/scala-js-dom/pull/78
          rendered
            .asInstanceOf[js.Dynamic]
            .options
            .asInstanceOf[js.Array[dom.HTMLSelectElement]]
            .apply(idx)
        }

        val mapping = mutable.Map.empty[Ref[String], HTML.Input.Select.Option]

        def onSelect(select: Option[Ref[T]]) {
          if (select.isEmpty) {
            if (optDefault.nonEmpty)
              optDefault.get.rendered.setAttribute("selected", "")
          } else (for {
            selection <- select
            ch <- map.mapping.get(selection)
            opt <- mapping.get(ch)
          } yield opt).foreach(_.rendered.setAttribute("selected", ""))
        }

        map.changes.attach {
          case Change.Insert(Position.Head(), element) =>
            mapping += element -> addOption(element.get)

            if (optDefault.isEmpty)
              rendered.insertBefore(
                mapping(element).rendered,
                rendered.firstChild)
            else
              rendered.insertBefore(
                mapping(element).rendered,
                optDefault.get.rendered)

            // TODO Flush only if selection was invalidated by Remove() or Clear()
            selection.flush(onSelect)

          case Change.Insert(Position.Last(), element) =>
            mapping += element -> addOption(element.get)
            rendered.appendChild(mapping(element).rendered)
            selection.flush(onSelect)

          case Change.Insert(Position.Before(reference), element) =>
            mapping += element -> addOption(element.get)
            rendered.insertBefore(
              mapping(element).rendered,
              mapping(reference).rendered)
            selection.flush(onSelect)

          case Change.Insert(Position.After(reference), element) =>
            mapping += element -> addOption(element.get)
            DOM.insertAfter(rendered,
              mapping(reference).rendered,
              mapping(element).rendered)
            selection.flush(onSelect)

          case Change.Update(reference, element) =>
            mapping += element -> addOption(element.get)
            rendered.replaceChild(
              mapping(element).rendered,
              mapping(reference).rendered)
            mapping -= reference

          case Change.Remove(element) =>
            rendered.removeChild(mapping(element).rendered)
            mapping -= element

          case Change.Clear() =>
            mapping.foreach { case (_, value) =>
              rendered.removeChild(value.rendered) }
            mapping.clear()
        }

        val obs = selection.attach(onSelect)

        change.attach { e =>
          val m = mapping.find(_._2.rendered == selected())

          if (m.isDefined) {
            val found = map.mapping.find(_._2 == m.get._1).get._1
            selection.produce(Some(found), obs)
          } else selection.produce(None, obs)
        }

        self
      }
    }
  }

  trait Container[V] extends Widget[V] { self: V =>
    def subscribe[T <: String](value: ReadChannel[T]) = {
      value.attach(cur => rendered.textContent = cur.toString)
      self
    }

    /** Subscribe raw HTML. */
    def raw[T](value: ReadChannel[String]) = {
      value.attach(rendered.innerHTML = _)
      self
    }

    def widget[T <: Widget[_]](value: ReadChannel[T]) = {
      value.attach { cur =>
        if (rendered.firstChild != null) rendered.removeChild(rendered.firstChild)
        rendered.appendChild(cur.rendered)
      }

      self
    }

    def optWidget[T <: Option[Widget[_]]](value: ReadChannel[T]) = {
      value.attach { cur =>
        if (rendered.firstChild != null) rendered.removeChild(rendered.firstChild)
        if (cur.isDefined) rendered.appendChild(cur.get.rendered)
      }

      self
    }
  }
}

object DOMChannel {
  /* TODO As an optimisation, f(null) should be called when all children
   * are detached from the channel. If a child gets added, then the callback
   * handler needs to get reinitialised again.
   */
  def event(f: (dom.Event => Unit) => Unit): Channel[dom.Event] = {
    val ev = Channel[dom.Event]()
    f((e: dom.Event) => ev.produce(e))
    ev
  }

  def keyboardEvent(f: (dom.KeyboardEvent => Unit) => Unit): Channel[dom.KeyboardEvent] = {
    val ev = Channel[dom.KeyboardEvent]()
    f((e: dom.KeyboardEvent) => ev.produce(e))
    ev
  }

  def mouseEvent(f: (dom.MouseEvent => Unit) => Unit): Channel[dom.MouseEvent] = {
    val ev = Channel[dom.MouseEvent]()
    f((e: dom.MouseEvent) => ev.produce(e))
    ev
  }

  def touchEvent(rendered: dom.HTMLElement, id: String): Channel[dom.TouchEvent] = {
    val ev = Channel[dom.TouchEvent]()
    rendered.addEventListener(id,
      (e: dom.Event) => ev.produce(e.asInstanceOf[dom.TouchEvent]),
      useCapture = false)
    ev
  }
}

trait View {
  def render(parent: dom.Node, offset: dom.Node)
}

case class Inline(contents: Widget[_]*) extends View {
  def render(parent: dom.Node, offset: dom.Node) {
    contents.foreach(_.render(parent, parent.lastChild))
  }
}

trait Node extends View {
  val rendered: dom.HTMLElement

  def render(parent: dom.Node, offset: dom.Node) {
    DOM.insertAfter(parent, offset, rendered)
  }

  lazy val click = DOMChannel.mouseEvent(rendered.onclick = _)
  lazy val doubleClick = DOMChannel.mouseEvent(rendered.ondblclick = _)
  lazy val mouseLeave = DOMChannel.mouseEvent(rendered.onmouseleave = _)
  lazy val mouseEnter = DOMChannel.mouseEvent(rendered.onmouseenter = _)
  lazy val mouseOut = DOMChannel.mouseEvent(rendered.onmouseout = _)
  lazy val mouseUp = DOMChannel.mouseEvent(rendered.onmouseup = _)
  lazy val mouseOver = DOMChannel.mouseEvent(rendered.onmouseover = _)
  lazy val mouseDown = DOMChannel.mouseEvent(rendered.onmousedown = _)
  lazy val mouseMove = DOMChannel.mouseEvent(rendered.onmousemove = _)
  lazy val contextMenu = DOMChannel.mouseEvent(rendered.oncontextmenu = _)

  lazy val keyUp = DOMChannel.keyboardEvent(rendered.onkeyup = _)
  lazy val keyDown = DOMChannel.keyboardEvent(rendered.onkeydown = _)
  lazy val keyPress = DOMChannel.keyboardEvent(rendered.onkeypress = _)

  lazy val touchStart = DOMChannel.touchEvent(rendered, "ontouchstart")
  lazy val touchMove = DOMChannel.touchEvent(rendered, "ontouchmove")
  lazy val touchEnd = DOMChannel.touchEvent(rendered, "ontouchend")

  lazy val change = DOMChannel.event(rendered.onchange = _)
}

object Node {
  def apply(node: dom.HTMLElement): Node =
    new Node {
      val rendered = node
    }
}

trait Widget[T] extends Node { self: T =>
  def onClick(f: dom.MouseEvent => Unit) = { click.attach(f); self }
  def onDoubleClick(f: dom.MouseEvent => Unit) = { doubleClick.attach(f); self }
  def onMouseLeave(f: dom.MouseEvent => Unit) = { mouseLeave.attach(f); self }
  def onMouseEnter(f: dom.MouseEvent => Unit) = { mouseEnter.attach(f); self }
  def onMouseOut(f: dom.MouseEvent => Unit) = { mouseOut.attach(f); self }
  def onMouseUp(f: dom.MouseEvent => Unit) = { mouseUp.attach(f); self }
  def onMouseOver(f: dom.MouseEvent => Unit) = { mouseOver.attach(f); self }
  def onMouseDown(f: dom.MouseEvent => Unit) = { mouseDown.attach(f); self }
  def onMouseMove(f: dom.MouseEvent => Unit) = { mouseMove.attach(f); self }
  def onContextMenu(f: dom.MouseEvent => Unit) = { contextMenu.attach(f); self }

  def onKeyUp(f: dom.KeyboardEvent => Unit) = { keyUp.attach(f); self }
  def onKeyDown(f: dom.KeyboardEvent => Unit) = { keyDown.attach(f); self }
  def onKeyPress(f: dom.KeyboardEvent => Unit) = { keyPress.attach(f); self }

  def onTouchStart(f: dom.TouchEvent => Unit) = { touchStart.attach(f); self }
  def onTouchMove(f: dom.TouchEvent => Unit) = { touchMove.attach(f); self }
  def onTouchEnd(f: dom.TouchEvent => Unit) = { touchEnd.attach(f); self }

  def onChange(f: dom.Event => Unit) = { change.attach(f); self }

  def id(id: String) = {
    rendered.id = id
    self
  }

  def cursor(cursor: HTML.Cursor) = {
    rendered.style.cursor = cursor.toString
    self
  }

  def css(cssTags: String*) = {
    val tags = rendered.className.split(" ").toSet
    rendered.className = (tags ++ cssTags).mkString(" ")
    self
  }

  def css(state: Boolean, cssTags: String*) = {
    val tags = rendered.className.split(" ").toSet

    val changed =
      if (state) tags ++ cssTags
      else tags.diff(cssTags.toSet)

    rendered.className = changed.mkString(" ")
    self
  }

  /** The tags produced by the channel may consist of multiple space-separated
    * CSS classes.
    */
  def cssCh(tag: ReadChannel[String]) = {
    var cur: Option[Set[String]] = None

    tag.attach { value =>
      val tags = rendered.className.split(" ").toSet
      val changed =
        if (cur.isDefined) tags.diff(cur.get) + value
        else tags + value
      cur = Some(value.split(" ").toSet)

      rendered.className = changed.mkString(" ")
    }

    self
  }

  def cssCh(state: ReadChannel[Boolean], cssTags: String*) = {
    state.attach(value => css(value, cssTags: _*))
    self
  }

  def attribute(key: String, value: String) = {
    rendered.setAttribute(key, value)
    self
  }

  def attributeCh(key: String, value: ReadChannel[Option[String]]) = {
    value.attach {
      case Some(v) => rendered.setAttribute(key, v)
      case None => rendered.removeAttribute(key)
    }

    self
  }

  def tabIndex(value: Int) = {
    rendered.setAttribute("tabindex", value.toString)
    self
  }

  def title(value: String) = {
    rendered.setAttribute("title", value)
    self
  }

  def titleCh(value: ReadChannel[String]) = {
    value.attach { title =>
      rendered.setAttribute("title", title)
    }

    self
  }

  def show(value: ReadChannel[Boolean], remove: Boolean = true) = {
    value.attach { cur =>
      if (remove) {
        rendered.style.display =
          if (cur) "" else "none"
      } else {
        rendered.style.visibility =
          if (cur) "visible" else "hidden"
      }
    }

    self
  }
}
