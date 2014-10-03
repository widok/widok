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
    def Text(placeholder: String = "", autofocus: Boolean = false, autocomplete: Boolean = true)(contents: Widget*) = new Widget {
      val rendered = tag("input", contents: _*)
        .asInstanceOf[HTMLInputElement]

      if (autofocus) rendered.setAttribute("autofocus", "")
      rendered.setAttribute("placeholder", placeholder)
      rendered.setAttribute("autocomplete", if (autocomplete) "on" else "off")
      rendered.setAttribute("type", "text")

      var enter: () => Unit = null

      // Provides two-way binding.
      def bind(value: Channel[String]) = {
        // TODO Add flag ``live`` to produce every single character
        val producer = () =>
          Some(rendered.value)
        val observer = (text: String) =>
          rendered.value = text.toString

        value.attach(producer)
        value.attach(observer)

        rendered.onkeyup = (e: KeyboardEvent) =>
          if (e.keyCode == 13) {
            value.produce(rendered.value, observer)
            if (enter != null) enter()
          }

        this
      }

      def onEnter(f: () => Unit) = {
        enter = f
        this
      }
    }

    case class Checkbox() extends Widget {
      val rendered = tag("input")
        .asInstanceOf[HTMLInputElement]
      rendered.setAttribute("type", "checkbox")

      def bind(value: Channel[Boolean], f: (Boolean, Checkbox) => Unit): Checkbox = {
        val producer = () =>
          Some(rendered.checked)
        val observer = (checked: Boolean) =>
          rendered.checked = checked

        value.attach(producer)
        value.attach(observer)

        rendered.onchange = (e: dom.Event) => {
          f(rendered.checked, this)
          value.produce(rendered.checked, observer)
        }

        this
      }

      def bind(value: Channel[Boolean]): Checkbox =
        bind(value, (a, b) => ())
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
    def bind[T, U <: Seq[T]](channel: Channel[U], f: (T, List.Item) => Widget) = {
      channel.attach(list => {
        DOM.clear(rendered)

        list.foreach { cur =>
          val li = List.Item()
          li.rendered.appendChild(f(cur, li).rendered)
          rendered.appendChild(li.rendered)
        }
      })

      this
    }

    def bind[T](aggregate: Aggregate[T], f: (Channel[T], List.Item) => Widget) = {
      var map = mutable.Map[Channel[T], dom.Node]()

      aggregate.attach(new Aggregate.Observer[T] {
        def append(cur: Channel[T]) {
          val li = List.Item()
          li.rendered.appendChild(f(cur, li).rendered)
          rendered.appendChild(li.rendered)
          map += (cur -> li.rendered)
        }

        def remove(cur: Channel[T]) {
          rendered.removeChild(map(cur))
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

    def bindMany[T](value: Channel[Seq[T]], f: T => Widget) = {
      value.attach(list => {
        DOM.clear(rendered)
        fromSeq(list, f)
      })

      this
    }

    // Binds using toString.
    def bind[T](value: Channel[T]) = {
      value.attach(cur => {
        DOM.clear(rendered)
        rendered.appendChild(dom.document.createTextNode(cur.toString))
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

    // Binds using a custom formatter, may construct complex widgets.
    def bind[T](value: Channel[T], f: PartialFunction[T, Widget]) = {
      value.attach(cur => {
        DOM.clear(rendered)
        val res = f.lift(cur)
        if (res.isDefined) rendered.appendChild(res.get.rendered)
      })

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
