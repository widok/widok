package org.widok.bindings

import org.widok._
import org.scalajs.dom
import org.scalajs.dom.HTMLInputElement

object HTML {
  trait Cursor
  object Cursor {
    case object Wait extends Cursor { override def toString = "wait" }
    case object Pointer extends Cursor { override def toString = "pointer" }
    case object Move extends Cursor { override def toString = "move" }
  }

  object Heading {
    case class Level1(contents: Widget[_]*) extends Widget[Level1] {
      val rendered = DOM.createElement("h1", contents: _*)
    }

    case class Level2(contents: Widget[_]*) extends Widget[Level2] {
      val rendered = DOM.createElement("h2", contents: _*)
    }

    case class Level3(contents: Widget[_]*) extends Widget[Level3] {
      val rendered = DOM.createElement("h3", contents: _*)
    }

    case class Level4(contents: Widget[_]*) extends Widget[Level4] {
      val rendered = DOM.createElement("h4", contents: _*)
    }

    case class Level5(contents: Widget[_]*) extends Widget[Level5] {
      val rendered = DOM.createElement("h5", contents: _*)
    }

    case class Level6(contents: Widget[_]*) extends Widget[Level6] {
      val rendered = DOM.createElement("h6", contents: _*)
    }
  }

  case class Paragraph(contents: Widget[_]*) extends Widget[Paragraph] {
    val rendered = DOM.createElement("p", contents: _*)
  }

  object Text {
    case class Bold(contents: Widget[_]*) extends Widget[Bold] {
      val rendered = DOM.createElement("b", contents: _*)
    }

    case class Small(contents: Widget[_]*) extends Widget[Small] {
      val rendered = DOM.createElement("small", contents: _*)
    }
  }

  case class Text(value: String) extends Widget[Text] {
    val rendered = dom.document.createTextNode(value)
      .asInstanceOf[org.scalajs.dom.HTMLElement]
  }

  case class Raw(html: String) extends Widget[Raw] {
    val rendered = DOM.createElement("span")
    rendered.innerHTML = html
  }

  case class Image(source: String) extends Widget[Image] {
    val rendered = DOM.createElement("img")
    rendered.setAttribute("src", source)
  }

  case class LineBreak() extends Widget[LineBreak] {
    val rendered = DOM.createElement("br")
  }

  case class Button(contents: Widget[_]*) extends Widget.Button[Button] {
    val rendered = DOM.createElement("button", contents: _*)
  }

  case class Section(contents: Widget[_]*) extends Widget[Section] {
    val rendered = DOM.createElement("section", contents: _*)
  }

  case class Header(contents: Widget[_]*) extends Widget[Header] {
    val rendered = DOM.createElement("header", contents: _*)
  }

  case class Footer(contents: Widget[_]*) extends Widget[Footer] {
    val rendered = DOM.createElement("footer", contents: _*)
  }

  case class Navigation(contents: Widget[_]*) extends Widget[Navigation] {
    val rendered = DOM.createElement("nav", contents: _*)
  }

  case class Anchor(contents: Widget[_]*) extends Widget.Anchor[Anchor] {
    val rendered = DOM.createElement("a", contents: _*)

    def url(value: String) = {
      rendered.setAttribute("href", value)
      this
    }
  }

  case class Form(contents: Widget[_]*) extends Widget[Form] {
    val rendered = DOM.createElement("form", contents: _*)
  }

  case class Label(contents: Widget[_]*) extends Widget[Label] {
    val rendered = DOM.createElement("label", contents: _*)

    def forId(value: String) = {
      rendered.setAttribute("for", value)
      this
    }
  }

  object Input {
    case class Text() extends Widget.Input.Text[Text] {
      val rendered = DOM.createElement("input")
        .asInstanceOf[HTMLInputElement]
      rendered.setAttribute("type", "text")

      def autofocus(value: Boolean) = {
        rendered.setAttribute("autofocus", "")
        this
      }

      def placeholder(value: String) = {
        rendered.setAttribute("placeholder", value)
        this
      }

      def autocomplete(value: Boolean) = {
        rendered.setAttribute("autocomplete", if (value) "on" else "off")
        this
      }
    }

    case class Checkbox() extends Widget.Input.Checkbox[Checkbox] {
      val rendered = DOM.createElement("input")
        .asInstanceOf[HTMLInputElement]
      rendered.setAttribute("type", "checkbox")
    }

    object Select {
      case class Option() extends Widget[Option] {
        val rendered = DOM.createElement("option")
          .asInstanceOf[dom.HTMLElement]

        def bind(ch: ReadChannel[Boolean]) = {
          val obs = ch.map { selected ⇒
            if (selected) Some("")
            else None
          }

          attributeCh("selected", obs)
        }
      }
    }

    case class Select(options: Seq[String] = Seq.empty, selected: Int = -1) extends Widget.Input.Select[Select] {
      val rendered = DOM.createElement("select")
      options.zipWithIndex.foreach { case (cur, idx) =>
        val elem = DOM.createElement("option")
        elem.appendChild(dom.document.createTextNode(cur))
        if (idx == selected) elem.setAttribute("selected", "")
        rendered.appendChild(elem)
      }
    }
  }

  case class HorizontalLine() extends Widget[HorizontalLine] {
    val rendered = DOM.createElement("hr")
  }

  object List {
    case class Unordered(contents: List.Item*) extends Widget.List[Unordered] {
      val rendered = DOM.createElement("ul", contents: _*)
    }

    case class Ordered(contents: List.Item*) extends Widget.List[Ordered] {
      val rendered = DOM.createElement("ol", contents: _*)
    }

    case class Item(contents: Widget[_]*) extends Widget.List.Item[Item] {
      val rendered = DOM.createElement("li", contents: _*)
    }
  }

  object Table {
    case class Head(contents: Row*) extends Widget.List[Head] {
      val rendered = DOM.createElement("thead", contents: _*)
    }

    case class HeadColumn(contents: Widget[_]*) extends Widget[HeadColumn] {
      val rendered = DOM.createElement("th", contents: _*)
    }

    case class Body(contents: Row*) extends Widget.List[Body] {
      val rendered = DOM.createElement("tbody", contents: _*)
    }

    // May contain either HeadColumn or Column
    case class Row(contents: Widget[_]*) extends Widget.List.Item[Row] {
      val rendered = DOM.createElement("tr", contents: _*)
    }

    case class Column(contents: Widget[_]*) extends Widget[Column] {
      val rendered = DOM.createElement("td", contents: _*)
    }
  }

  case class Table(contents: Widget[_]*) extends Widget[Table] {
    val rendered = DOM.createElement("table", contents: _*)
  }

  object Container {
    case class Generic(contents: Widget[_]*) extends Widget.Container[Generic] {
      val rendered = DOM.createElement("div", contents: _*)
    }

    case class Inline(contents: Widget[_]*) extends Widget.Container[Inline] {
      val rendered = DOM.createElement("span", contents: _*)
    }
  }

  object IterableContainer {
    case class Generic(contents: Item*) extends Widget.List[Generic] {
      val rendered = DOM.createElement("div", contents: _*)
    }

    case class Inline(contents: Item*) extends Widget.List[Inline] {
      val rendered = DOM.createElement("span", contents: _*)
    }

    case class Item(contents: Widget[_]*) extends Widget.List.Item[Item] {
      val rendered = DOM.createElement("span", contents: _*)
    }
  }
}