package org.widok

import org.scalajs.dom
import org.scalajs.dom.ext.KeyCode

import org.widok.bindings._

import scala.collection.mutable

object Widget {
  object List {
    trait Item[V] extends Widget[V] { self: V =>

    }
  }

  trait List[V] extends Widget[V] { self: V =>

  }

  object Input {
    trait Text[V] extends Widget[V] { self: V =>
      val rendered: dom.html.Input

      /** Produce current value after every key press. */
      lazy val value = PtrVar[String](
        keyUp | paste | blur | change, rendered.value, rendered.value = _)

      /** Produce current value after enter was pressed. */
      lazy val enterValue = PtrVar[String](
        keyUp.filter(_.keyCode == KeyCode.enter),
        rendered.value, rendered.value = _)

      def bind(ch: Channel[String]) = { value.bind(ch); self }
      def subscribe(ch: ReadChannel[String]) = { value.subscribe(ch); self }
      def attach(f: String => Unit) = { value.attach(f); self }

      def bindEnter(ch: Channel[String]) = { enterValue.bind(ch); self }
      def subscribeEnter(ch: ReadChannel[String]) = { enterValue.subscribe(ch); self }
      def attachEnter(f: String => Unit) = { enterValue.attach(f); self }
    }

    trait Checkbox[V] extends Widget[V] { self: V =>
      val rendered: dom.html.Input

      lazy val checked = PtrVar[Boolean](change,
        rendered.checked, rendered.checked = _)

      def bind(ch: Channel[Boolean]) = { checked.bind(ch); self }
      def subscribe(ch: ReadChannel[Boolean]) = { checked.subscribe(ch); self }
      def attach(f: Boolean => Unit) = { checked.attach(f); self }
    }

    trait Select[V] extends Widget[V] { self: V =>
      private var optDefault = Option.empty[HTML.Input.Select.Option]

      /* All manually added elements will be ignored by bind() */
      def default(caption: String) = {
        assert(optDefault.isEmpty, "default() can only be used once")
        assert(!rendered.hasChildNodes(), "DOM modified externally")
        val opt = HTML.Input.Select.Option(caption)
        rendered.appendChild(opt.rendered)
        optDefault = Some(opt)
        self
      }

      def bind[T, X <: List.Item[X]](buf: DeltaBuffer[T],
                                     f: T => HTML.Input.Select.Option,
                                     selection: Channel[Option[T]]) =
      {
        import Buffer.Delta
        import Buffer.Position

        def selected(): dom.html.Select = {
          val castRendered = rendered.asInstanceOf[dom.html.Select]
          castRendered.options(castRendered.selectedIndex)
        }

        val mapping = mutable.Map.empty[T, HTML.Input.Select.Option]

        def onSelect(select: Option[T]) {
          val node: Option[HTML.Input.Select.Option] =
            if (select.isDefined) mapping.get(select.get)
            else if (optDefault.nonEmpty) optDefault
            else None

          node.foreach(_.attribute("selected", ""))
        }

        buf.changes.attach {
          case Delta.Insert(Position.Head(), element) =>
            mapping += element -> f(element)

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

          case Delta.Insert(Position.Last(), element) =>
            mapping += element -> f(element)
            rendered.appendChild(mapping(element).rendered)
            selection.flush(onSelect)

          case Delta.Insert(Position.Before(reference), element) =>
            mapping += element -> f(element)
            rendered.insertBefore(
              mapping(element).rendered,
              mapping(reference).rendered)
            selection.flush(onSelect)

          case Delta.Insert(Position.After(reference), element) =>
            mapping += element -> f(element)
            DOM.insertAfter(rendered,
              mapping(reference).rendered,
              mapping(element).rendered)
            selection.flush(onSelect)

          case Delta.Replace(reference, element) =>
            mapping += element -> f(element)
            rendered.replaceChild(
              mapping(element).rendered,
              mapping(reference).rendered)
            mapping -= reference

          case Delta.Remove(element) =>
            rendered.removeChild(mapping(element).rendered)
            mapping -= element

          case Delta.Clear() =>
            mapping.foreach { case (_, value) =>
              rendered.removeChild(value.rendered) }
            mapping.clear()
        }

        val obs = selection.attach(onSelect)

        change.attach { e =>
          val m = mapping.find(_._2.rendered == selected())
          selection.produce(m.map(_._1), obs)
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
  }
}

object DOMChannel {
  def variable[T](set: T => Unit) = {
    val opt = Opt[T]()
    opt.attach(set)
    opt
  }

  /* TODO As an optimisation, f(null) should be called when all children
   * are detached from the channel. If a child gets added, then the callback
   * handler needs to get reinitialised again.
   */
  def event(f: (dom.Event => Unit) => Unit): Channel[dom.Event] = {
    val ev = Channel[dom.Event]()
    f((e: dom.Event) => ev.produce(e))
    ev
  }

  def dragEvent(f: (dom.DragEvent => Unit) => Unit): Channel[dom.DragEvent] = {
    val ev = Channel[dom.DragEvent]()
    f((e: dom.DragEvent) => ev.produce(e))
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

  def touchEvent(rendered: dom.html.Element, id: String): Channel[dom.TouchEvent] = {
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

case class PlaceholderWidget[T <: Widget[_]](value: ReadChannel[T]) extends View {
  def render(parent: dom.Node, offset: dom.Node) {
    var node = DOM.createElement(null)
    DOM.insertAfter(parent, offset, node)

    value.attach { cur =>
      parent.replaceChild(cur.rendered, node)
      node = cur.rendered
    }
  }
}

case class PlaceholderOptWidget[T <: Option[Widget[_]]](value: ReadChannel[T]) extends View {
  def render(parent: dom.Node, offset: dom.Node) {
    var node = DOM.createElement(null)
    DOM.insertAfter(parent, offset, node)

    value.attach { (t: Option[Widget[_]]) =>
      t match {
        case Some(widget) =>
          parent.replaceChild(widget.rendered, node)
          node = widget.rendered
        case None =>
          val empty = DOM.createElement(null)
          parent.replaceChild(empty, node)
          node = empty
      }
    }
  }
}

trait Length
object Length {
  case class Pixel(value: Double) extends Length { override def toString = s"${value}px" }
  case class Element(value: Double) extends Length { override def toString = s"${value}em" }
  case class Percentage(value: Double) extends Length { override def toString = s"${value * 100}%" }
}

case class CSSStyle(style: dom.css.StyleDeclaration) {
  lazy val display: Opt[String] = DOMChannel.variable(style.display = _)
  lazy val visibility: Opt[String] = DOMChannel.variable(style.visibility = _)
  lazy val cursor: Opt[HTML.Cursor] = DOMChannel.variable(v => style.cursor = v.toString)
  lazy val height: Opt[Length] = DOMChannel.variable(v => style.height = v.toString)
  lazy val width: Opt[Length] = DOMChannel.variable(v => style.width = v.toString)
}

trait Node extends View {
  val rendered: dom.html.Element

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

  lazy val paste = DOMChannel.dragEvent(rendered.onpaste = _)
  lazy val drag = DOMChannel.dragEvent(rendered.ondrag = _)
  lazy val dragStart = DOMChannel.dragEvent(rendered.ondragstart = _)
  lazy val dragEnd = DOMChannel.dragEvent(rendered.ondragend = _)
  lazy val dragEnter = DOMChannel.dragEvent(rendered.ondragenter = _)
  lazy val dragOver = DOMChannel.dragEvent(rendered.ondragover = _)
  lazy val dragLeave = DOMChannel.dragEvent(rendered.ondragleave = _)
  lazy val drop = DOMChannel.dragEvent(rendered.ondrop = _)

  lazy val keyUp = DOMChannel.keyboardEvent(rendered.onkeyup = _)
  lazy val keyDown = DOMChannel.keyboardEvent(rendered.onkeydown = _)
  lazy val keyPress = DOMChannel.keyboardEvent(rendered.onkeypress = _)

  lazy val touchStart = DOMChannel.touchEvent(rendered, "ontouchstart")
  lazy val touchMove = DOMChannel.touchEvent(rendered, "ontouchmove")
  lazy val touchEnd = DOMChannel.touchEvent(rendered, "ontouchend")

  lazy val change = DOMChannel.event(rendered.onchange = _)
  lazy val blur = DOMChannel.event(rendered.onblur = _)

  lazy val nodeId: Opt[String] = DOMChannel.variable(rendered.id = _)

  lazy val submit = DOMChannel.event(rendered.onsubmit = _)

  lazy val className = {
    val set = BufSet[String]()
    set.toSeq.attach { tags =>
      assert(tags.forall(!_.contains(" ")), s"A tag in '$tags' contains spaces")
      rendered.className = tags.mkString(" ")
    }
    set
  }

  lazy val style = CSSStyle(rendered.style)

  lazy val attributes = {
    val dict = Dict[String, String]()

    dict.changes.attach {
      case Dict.Delta.Insert(k, v) => rendered.setAttribute(k, v)
      case Dict.Delta.Update(k, v) => rendered.setAttribute(k, v)
      case Dict.Delta.Remove(k) => rendered.removeAttribute(k)
      case Dict.Delta.Clear() => rendered.removeAttribute()
    }

    dict
  }
}

object Node {
  def apply(node: dom.html.Element): Node =
    new Node {
      val rendered = node
    }
}

/**
 * Convenience trait to use the channels from [[Node]] with chained function
 * calls.
 */
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

  def onPaste(f: dom.DragEvent => Unit) = { paste.attach(f); self }
  def onDrag(f: dom.DragEvent => Unit) = { drag.attach(f); self }
  def onDragStart(f: dom.DragEvent => Unit) = { dragStart.attach(f); self }
  def onDragEnd(f: dom.DragEvent => Unit) = { dragEnd.attach(f); self }
  def onDragEnter(f: dom.DragEvent => Unit) = { dragEnter.attach(f); self }
  def onDragOver(f: dom.DragEvent => Unit) = { dragOver.attach(f); self }
  def onDragLeave(f: dom.DragEvent => Unit) = { dragLeave.attach(f); self }
  def onDrop(f: dom.DragEvent => Unit) = { drop.attach(f); self }

  def onKeyUp(f: dom.KeyboardEvent => Unit) = { keyUp.attach(f); self }
  def onKeyDown(f: dom.KeyboardEvent => Unit) = { keyDown.attach(f); self }
  def onKeyPress(f: dom.KeyboardEvent => Unit) = { keyPress.attach(f); self }

  def onTouchStart(f: dom.TouchEvent => Unit) = { touchStart.attach(f); self }
  def onTouchMove(f: dom.TouchEvent => Unit) = { touchMove.attach(f); self }
  def onTouchEnd(f: dom.TouchEvent => Unit) = { touchEnd.attach(f); self }

  def onSubmit(f: dom.Event => Unit) = { submit.attach(f); self }

  def onChange(f: dom.Event => Unit) = { change.attach(f); self }

  def focus(): T = { rendered.focus(); this }

  def id(value: String): T = { nodeId := value; self }

  def id(value: ReadChannel[String]): T = { nodeId.subscribe(value); self }

  def css(cssTags: String*): T = {
    cssTags.filterNot(className.contains$).foreach(className.insert)
    self
  }

  def css(tags: ReadChannel[Set[String]]): T = {
    tags.attach(className.set)
    self
  }

  def cssState(state: Boolean, cssTags: String*): T = {
    if (state) cssTags.filterNot(className.contains$).foreach(className.insert)
    else cssTags.filter(className.contains$).foreach(className.remove)
    self
  }

  def cssState(state: ReadChannel[Boolean], cssTags: String*): T = {
    state.attach(value => cssState(value, cssTags: _*))
    self
  }

  def attribute(key: String, value: String): T = {
    attributes.insertOrUpdate(key, value)
    self
  }

  def attribute(key: String, value: ReadChannel[String]): T = {
    value.attach(attributes.insertOrUpdate(key, _))
    self
  }

  def attributeOpt(key: String, value: PartialChannel[String]): T = {
    value.values.attach {
      case Some(v) => attributes.insertOrUpdate(key, v)
      case None => attributes.removeIfExists(key)
    }

    self
  }

  def tabIndex(value: Int): T = attribute("tabindex", value.toString)
  def tabIndex(value: ReadChannel[Int]): T = { value.attach(tabIndex(_)); self }

  def title(value: String): T = attribute("title", value)
  def title(value: ReadChannel[String]): T = attribute("title", value)

  def cursor(cursor: HTML.Cursor): T = { style.cursor := cursor; self }
  def cursor(cursor: ReadChannel[HTML.Cursor]): T = {
    style.cursor.subscribe(cursor)
    self
  }

  def height(height: Length): T = { style.height := height; self }
  def height(height: ReadChannel[Length]): T = {
    style.height.subscribe(height)
    self
  }

  def width(width: Length): T = { style.width := width; self }
  def width(width: ReadChannel[Length]): T = {
    style.width.subscribe(width)
    self
  }

  def show(value: Boolean): T = {
    style.display := (if (value) "" else "none")
    self
  }

  def show(value: ReadChannel[Boolean]): T = {
    value.attach(show(_))
    self
  }

  def visible(value: Boolean): T = {
    style.visibility := (if (value) "visible" else "hidden")
    self
  }

  def visible(value: ReadChannel[Boolean]): T = {
    value.attach(visible(_))
    self
  }

  def enabled(value: Boolean): T = {
    if (value) attributes.removeIfExists("disabled")
    else attributes.insertOrUpdate("disabled", "")
    self
  }

  def enabled(value: ReadChannel[Boolean]): T = {
    value.attach(enabled(_))
    self
  }

  /** Coordinates relative to the padding box of offsetParent */
  def relativeCoordinates =
    Coordinates(
      width = rendered.offsetWidth,
      height = rendered.offsetHeight,
      top = rendered.offsetTop,
      left = rendered.offsetLeft)

  /** Adds child widget */
  def append(widget: Widget[_]) {
    rendered.appendChild(widget.rendered)
  }

  /** Removes child widget */
  def remove(widget: Widget[_]) {
    rendered.removeChild(widget.rendered)
  }

  // TODO Overloading the two operators leads to a regression in the JavaScript
  // test cases.

  // def +=(widget: Widget[_]) = append(widget)
  // def -=(widget: Widget[_]) = remove(widget)
}
