package org.widok.bindings

import org.widok._
import org.scalajs.dom
import org.scalajs.dom.{HTMLInputElement, KeyboardEvent}

import scala.collection.mutable

object HTML {
  trait Cursor
  object Cursor {
    case object Wait extends Cursor { override def toString = "wait" }
    case object Pointer extends Cursor { override def toString = "pointer" }
    case object Move extends Cursor { override def toString = "move" }
  }

  object Heading {
    case class Level1(contents: Widget*) extends Widget {
      val rendered = tag("h1", contents: _*)
    }

    case class Level2(contents: Widget*) extends Widget {
      val rendered = tag("h2", contents: _*)
    }

    case class Level3(contents: Widget*) extends Widget {
      val rendered = tag("h3", contents: _*)
    }

    case class Level4(contents: Widget*) extends Widget {
      val rendered = tag("h4", contents: _*)
    }

    case class Level5(contents: Widget*) extends Widget {
      val rendered = tag("h5", contents: _*)
    }

    case class Level6(contents: Widget*) extends Widget {
      val rendered = tag("h6", contents: _*)
    }
  }

  case class Paragraph(contents: Widget*) extends Widget {
    val rendered = tag("p", contents: _*)
  }

  object Text {
    case class Bold(contents: Widget*) extends Widget {
      val rendered = tag("b", contents: _*)
    }

    case class Small(contents: Widget*) extends Widget {
      val rendered = tag("small", contents: _*)
    }
  }

  case class Raw(html: String) extends Widget {
    val rendered = {
      val elem = tag("span")
      elem.innerHTML = html
      elem
    }
  }

  case class Image(source: String) extends Widget {
    val rendered = tag("img")
    rendered.setAttribute("src", source)
  }

  case class LineBreak() extends Widget {
    val rendered = tag("br")
  }

  case class Button(contents: Widget*) extends Widget {
    val rendered = tag("button", contents: _*)
  }

  case class Section(contents: Widget*) extends Widget {
    val rendered = tag("section", contents: _*)
  }

  case class Header(contents: Widget*) extends Widget {
    val rendered = tag("header", contents: _*)
  }

  case class Footer(contents: Widget*) extends Widget {
    val rendered = tag("footer", contents: _*)
  }

  case class Navigation(contents: Widget*) extends Widget {
    val rendered = tag("nav", contents: _*)
  }

  def Anchor(ref: String = "")(contents: Widget*) = new Widget {
    val rendered = tag("a", contents: _*)

    if (ref == "") rendered.setAttribute("style", "cursor: pointer")
    else rendered.setAttribute("href", ref)
  }

  def Form(contents: Widget*) = new Widget {
    val rendered = tag("form", contents: _*)
  }

  def Label(forId: String = "")(contents: Widget*) = new Widget {
    val rendered = tag("label", contents: _*)
    if (forId.nonEmpty) rendered.setAttribute("for", forId)
  }

  object Input {
    case class Text(placeholder: String = "", autofocus: Boolean = false, autocomplete: Boolean = true) extends Widget {
      val rendered = tag("input")
        .asInstanceOf[HTMLInputElement]

      if (autofocus) rendered.setAttribute("autofocus", "")
      rendered.setAttribute("placeholder", placeholder)
      rendered.setAttribute("autocomplete", if (autocomplete) "on" else "off")
      rendered.setAttribute("type", "text")

      /**
       * Provides two-way binding.
       *
       * @param readChannel
       *              The channel to read from.
       * @param writeChannel
       *              The channel to write to.
       * @param live
       *             Produce every single character if true, otherwise
       *             produce only if enter was pressed.
       * @return
       */
      def bind(readChannel: Channel[String], writeChannel: Channel[String], live: Boolean): Text = {
        readChannel.attach(() => Some(rendered.value)) // Producer
        readChannel.attach((text: String) => rendered.value = text) // Observer

        rendered.onkeyup = (e: KeyboardEvent) =>
          if (e.keyCode == 13 || live)
            writeChannel.produce(rendered.value)

        this
      }

      def bind(readWriteChannel: Channel[String], live: Boolean = false): Text =
        bind(readWriteChannel, (value: String) => readWriteChannel.produce(value), live)
    }

    case class Checkbox() extends Widget {
      val rendered = tag("input")
        .asInstanceOf[HTMLInputElement]
      rendered.setAttribute("type", "checkbox")

      def bind(readChannel: Channel[Boolean], writeChannel: Channel[Boolean]): Checkbox = {
        readChannel.attach(() => Some(rendered.checked)) // Producer
        readChannel.attach((checked: Boolean) => rendered.checked = checked) // Observer

        rendered.onchange = (e: dom.Event) =>
          writeChannel.produce(rendered.checked)

        this
      }

      def bind(readWriteChannel: Channel[Boolean]): Checkbox =
        bind(readWriteChannel, (value: Boolean) => readWriteChannel.produce(value))
    }

    case class Select(options: Seq[String], selected: Int = -1) extends Widget {
      // TODO define bind()
      val rendered = tag("select")
      options.zipWithIndex.foreach { case (cur, idx) =>
        val elem = dom.document.createElement("option")
        elem.appendChild(dom.document.createTextNode(cur))
        if (idx == selected) elem.setAttribute("selected", "")
        rendered.appendChild(elem)
      }
    }

  }

  trait List extends Widget {
    def bind[T, U <: Seq[T]](channel: Channel[U], f: T => Widget) = {
      channel.attach(list => {
        DOM.clear(rendered)

        list.foreach { cur =>
          rendered.appendChild(f(cur).rendered)
        }
      })

      this
    }

    def bind[T](aggregate: Aggregate[T])(f: Channel[T] => Widget) = {
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

  case class HorizontalLine() extends Widget {
    val rendered = tag("hr")
  }

  object List {

    case class Unordered(contents: Widget*) extends List {
      val rendered = tag("ul", contents: _*)
    }

    case class Ordered(contents: Widget*) extends List {
      val rendered = tag("ol", contents: _*)
    }

    case class Item(contents: Widget*) extends Widget {
      val rendered = tag("li", contents: _*)
    }

  }

  trait Container extends Widget {
    def fromSeq[T](items: Seq[T], f: T => Widget) = {
      items.foreach { cur =>
        rendered.appendChild(f(cur).rendered)
      }

      this
    }

    def bindString[T <: String](value: Channel[T]) = {
      value.attach(cur => rendered.textContent = cur.toString)
      value.populate()
      this
    }

    def bindInt[T <: Int](value: Channel[T]) = {
      value.attach(cur => rendered.textContent = cur.toString)
      value.populate()
      this
    }

    def bindDouble[T <: Double](value: Channel[T]) = {
      value.attach(cur => rendered.textContent = cur.toString)
      value.populate()
      this
    }

    def bindBoolean[T <: Boolean](value: Channel[T]) = {
      value.attach(cur => rendered.textContent = cur.toString)
      value.populate()
      this
    }

    def bindWidget[T <: Widget](value: Channel[T]) = {
      value.attach(cur => {
        if (rendered.firstChild != null) rendered.removeChild(rendered.firstChild)
        rendered.appendChild(cur.rendered)
      })

      value.populate()
      this
    }

    def bindOptWidget[T <: Option[Widget]](value: Channel[T]) = {
      value.attach(cur => {
        if (rendered.firstChild != null) rendered.removeChild(rendered.firstChild)
        if (cur.isDefined) rendered.appendChild(cur.get.rendered)
      })

      value.populate()
      this
    }

    // Bind HTML.
    def bindRaw[T](value: Channel[String]) = {
      value.attach(cur => {
        rendered.innerHTML = cur
      })

      value.populate()
      this
    }

    def bindMany[T](value: Channel[Seq[T]], f: T => Widget) = {
      value.attach(list => {
        DOM.clear(rendered)
        fromSeq(list, f)
      })

      value.populate()
      this
    }
  }

  object Container {

    case class Generic(contents: Widget*) extends Container {
      val rendered = tag("div", contents: _*)
    }

    case class Inline(contents: Widget*) extends Container {
      val rendered = tag("span", contents: _*)
    }

  }
}
