package org.widok.bindings

import org.widok._
import org.scalajs.dom

import scala.collection.mutable

object HTML {
  trait Cursor
  object Cursor {
    case object Wait extends Cursor { override def toString = "wait" }
    case object Pointer extends Cursor { override def toString = "pointer" }
    case object Move extends Cursor { override def toString = "move" }
  }

  object Heading {
    case class Level1(contents: View*) extends Widget[Level1] {
      val rendered = DOM.createElement("h1", contents)
    }

    case class Level2(contents: View*) extends Widget[Level2] {
      val rendered = DOM.createElement("h2", contents)
    }

    case class Level3(contents: View*) extends Widget[Level3] {
      val rendered = DOM.createElement("h3", contents)
    }

    case class Level4(contents: View*) extends Widget[Level4] {
      val rendered = DOM.createElement("h4", contents)
    }

    case class Level5(contents: View*) extends Widget[Level5] {
      val rendered = DOM.createElement("h5", contents)
    }

    case class Level6(contents: View*) extends Widget[Level6] {
      val rendered = DOM.createElement("h6", contents)
    }
  }

  case class Paragraph(contents: View*) extends Widget[Paragraph] {
    val rendered = DOM.createElement("p", contents)
  }

  object Text {
    case class Bold(contents: View*) extends Widget[Bold] {
      val rendered = DOM.createElement("b", contents)
    }

    case class Italic(contents: View*) extends Widget[Italic] {
      val rendered = DOM.createElement("i", contents)
    }

    case class Small(contents: View*) extends Widget[Small] {
      val rendered = DOM.createElement("small", contents)
    }
  }

  case class Text(value: String) extends Widget[Text] {
    val rendered = dom.document.createTextNode(value)
      .asInstanceOf[dom.html.Element]
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

  case class Button(contents: View*) extends Widget[Button] {
    val rendered = DOM.createElement("button", contents)
  }

  case class Section(contents: View*) extends Widget[Section] {
    val rendered = DOM.createElement("section", contents)
  }

  case class Header(contents: View*) extends Widget[Header] {
    val rendered = DOM.createElement("header", contents)
  }

  case class Footer(contents: View*) extends Widget[Footer] {
    val rendered = DOM.createElement("footer", contents)
  }

  case class Navigation(contents: View*) extends Widget[Navigation] {
    val rendered = DOM.createElement("nav", contents)
  }

  case class Anchor(contents: View*) extends Widget[Anchor] {
    val rendered = DOM.createElement("a", contents)

    def url(value: String) = {
      rendered.setAttribute("href", value)
      this
    }
  }

  case class Form(contents: View*) extends Widget[Form] {
    val rendered = DOM.createElement("form", contents)
  }

  case class Label(contents: View*) extends Widget[Label] {
    val rendered = DOM.createElement("label", contents)

    def forId(value: String) = {
      rendered.setAttribute("for", value)
      this
    }
  }

  object Input {
    trait Textual[T] extends Widget.Input.Text[T] { self: T =>
      val rendered = DOM.createElement("input")
        .asInstanceOf[dom.html.Input]

      def autofocus(value: Boolean) = {
        rendered.setAttribute("autofocus", "")
        self
      }

      def placeholder(value: String) = {
        rendered.setAttribute("placeholder", value)
        self
      }
    }

    trait TextBase[T] extends Textual[T] { self: T =>
      rendered.setAttribute("type", "text")

      def autocomplete(value: Boolean) = {
        rendered.setAttribute("autocomplete", if (value) "on" else "off")
        self
      }
    }

    case class Text() extends TextBase[Text]

    trait PasswordBase[T] extends Textual[T] { self: T =>
      rendered.setAttribute("type", "password")
    }

    case class Password() extends PasswordBase[Password]

    case class Checkbox() extends Widget.Input.Checkbox[Checkbox] {
      val rendered = DOM.createElement("input")
        .asInstanceOf[dom.html.Input]
      rendered.setAttribute("type", "checkbox")
    }

    case class Radio() extends Widget.Input.Checkbox[Radio] {
      val rendered = DOM.createElement("input")
        .asInstanceOf[dom.html.Input]
      rendered.setAttribute("type", "radio")
    }

    case class Number() extends Textual[Number] {
      rendered.setAttribute("type", "number")
    }

    case class Textarea() extends Widget.Input.Text[Textarea] {
      val rendered = DOM.createElement("textarea")
        .asInstanceOf[dom.html.Input]

      def cols(value: Int) = attribute("cols", value.toString)
      def rows(value: Int) = attribute("rows", value.toString)
    }

    case class File() extends Widget[File] {
      val rendered = DOM.createElement("input")
        .asInstanceOf[dom.html.Input]

      def accept(value: String) = {
        rendered.setAttribute("accept", value)
        this
      }

      def bind(writeChannel: WriteChannel[String]) = {
        rendered.addEventListener(
          "change",
          (e: dom.Event) => writeChannel.produce(rendered.value),
          useCapture = false)
        this
      }

      rendered.setAttribute("type", "file")
    }

    object Select {
      case class Option(caption: String) extends Widget[Option] {
        val rendered = DOM.createElement("option", Seq(HTML.Text(caption)))

        def bind(ch: ReadChannel[Boolean]) = {
          ch.attach { selected =>
            if (selected) attributes.insertOrUpdate("selected", "")
            else attributes.removeIfExists("selected")
          }
        }
      }
    }

    trait SelectBase[T] extends Widget.Input.Select[T] { self: T =>
      val rendered = DOM.createElement("select")

      def options(options: Seq[String], selected: Int = -1) = {
        options.zipWithIndex.foreach { case (cur, idx) =>
          val elem = DOM.createElement("option")
          elem.appendChild(dom.document.createTextNode(cur))
          if (idx == selected) elem.setAttribute("selected", "")
          rendered.appendChild(elem)
        }

        self
      }
    }

    case class Select() extends SelectBase[Select]
  }

  case class HorizontalLine() extends Widget[HorizontalLine] {
    val rendered = DOM.createElement("hr")
  }

  object List {
    case class Unordered(contents: Widget.List.Item[_]*) extends Widget.List[Unordered, List.Item] {
      val rendered = DOM.createElement("ul", contents)
    }

    case class Ordered(contents: Widget.List.Item[_]*) extends Widget.List[Ordered, List.Item] {
      val rendered = DOM.createElement("ol", contents)
    }

    case class Item(contents: View*) extends Widget.List.Item[Item] {
      val rendered = DOM.createElement("li", contents)
    }

    case class Items(buf: DeltaBuffer[Widget[_]]) extends Widget.List.Item[Items] {
      val rendered = DOM.createElement(null)

      override def render(parent: dom.Node, offset: dom.Node) {
        import Buffer.Delta
        import Buffer.Position

        DOM.insertAfter(parent, offset, rendered)

        var last: dom.Node = rendered

        buf.changes.attach {
          case Delta.Insert(Position.Head(), element) =>
            DOM.insertAfter(parent, rendered, element.rendered)
            if (last == rendered) last = element.rendered

          case Delta.Insert(Position.Last(), element) =>
            DOM.insertAfter(parent, last, element.rendered)
            last = element.rendered

          case Delta.Insert(Position.Before(reference), element) =>
            parent.insertBefore(element.rendered, reference.rendered)

          case Delta.Insert(Position.After(reference), element) =>
            DOM.insertAfter(parent, reference.rendered, element.rendered)
            if (last == reference.rendered) last = element.rendered

          case Delta.Replace(reference, element) =>
            parent.replaceChild(element.rendered, reference.rendered)

          case Delta.Remove(element) =>
            if (last == element.rendered) last = element.rendered.previousSibling
            parent.removeChild(element.rendered)

          case Delta.Clear() =>
            if (last != rendered) {
              DOM.remove(parent, rendered.nextSibling, last)
              last = rendered
            }
        }
      }
    }
  }

  object Table {
    case class Head(contents: RowBase[_]*) extends Widget.List[Head, Row] {
      val rendered = DOM.createElement("thead", contents)
    }

    case class HeadColumn(contents: View*) extends Widget[HeadColumn] {
      val rendered = DOM.createElement("th", contents)
    }

    trait RowBase[T] extends Widget[T] { self: T =>

    }

    case class Body(contents: RowBase[_]*) extends Widget.List[Body, RowBase[_]] {
      val rendered = DOM.createElement("tbody", contents)
    }

    /** May contain either HeadColumn or Column. */
    case class Row(contents: View*) extends RowBase[Row] {
      val rendered = DOM.createElement("tr", contents)
    }

    case class Column(contents: View*) extends Widget[Column] {
      val rendered = DOM.createElement("td", contents)
    }
  }

  case class Table(contents: View*) extends Widget[Table] {
    val rendered = DOM.createElement("table", contents)
  }

  object Container {
    case class Generic(contents: View*) extends Widget.Container[Generic] {
      val rendered = DOM.createElement("div", contents)
    }

    case class Inline(contents: View*) extends Widget.Container[Inline] {
      val rendered = DOM.createElement("span", contents)
    }
  }
}