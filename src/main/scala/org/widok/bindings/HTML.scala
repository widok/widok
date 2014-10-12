package org.widok.bindings

import org.widok._
import org.scalajs.dom
import org.scalajs.dom.{HTMLInputElement, KeyboardEvent}

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
       * @param data
       *              The channel to read from and to.
       * @param flush
       *              If the channel produces data, this flushes the data.
       * @param live
       *             Produce every single character if true, otherwise
       *             produce only if enter was pressed.
       * @return
       */
      def bind(data: Channel[String], flush: Channel[Nothing] = Channel(), live: Boolean = false): Text = {
        data.attach(text => rendered.value = text)
        flush.attach(_ => data := rendered.value)

        rendered.onkeyup = (e: KeyboardEvent) =>
          if (e.keyCode == 13 || live)
            data := rendered.value

        this
      }
    }

    case class Checkbox() extends Widget {
      val rendered = tag("input")
        .asInstanceOf[HTMLInputElement]
      rendered.setAttribute("type", "checkbox")

      def bind(data: Channel[Boolean], flush: Channel[Nothing] = Channel()): Checkbox = {
        data.attach(checked => rendered.checked = checked)
        flush.attach(_ => data := rendered.checked)

        rendered.onchange = (e: dom.Event) =>
          data := rendered.checked

        this
      }
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

  case class HorizontalLine() extends Widget {
    val rendered = tag("hr")
  }

  object List {
    case class Unordered(contents: List.Item*) extends Widget.List {
      val rendered = tag("ul", contents: _*)
    }

    case class Ordered(contents: List.Item*) extends Widget.List {
      val rendered = tag("ol", contents: _*)
    }

    case class Item(contents: Widget*) extends Widget.List.Item {
      val rendered = tag("li", contents: _*)
    }
  }

  object Container {
    case class Generic(contents: Widget*) extends Widget.Container {
      val rendered = tag("div", contents: _*)
    }

    case class Inline(contents: Widget*) extends Widget.Container {
      val rendered = tag("span", contents: _*)
    }
  }
}
