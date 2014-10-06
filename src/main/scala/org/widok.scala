package org

import org.scalajs.dom
import scala.scalajs.js
import org.widok.bindings.HTML

import scala.annotation.elidable
import elidable._

package object widok {
  @elidable(INFO) def log(values: Any*) {
    values.foreach(cur => dom.console.log(cur.asInstanceOf[js.Any]))
  }

  @elidable(SEVERE) def error(values: Any*) {
    values.foreach(cur => dom.console.error(cur.asInstanceOf[js.Any]))
  }

  @elidable(WARNING) def stub() {
    error("stub")
  }

  @elidable(INFO) def trace() {
    new Error().printStackTrace()
  }

  implicit def WidgetToSeq(view: Widget) = Seq(view)

  implicit def StringToWidget(s: String) = new Widget {
    val rendered = dom.document.createTextNode(s)
      // TODO This conversion is not well-defined, but needed as a workaround.
      .asInstanceOf[org.scalajs.dom.HTMLElement]
  }

  implicit def BooleanChannelToWidget[T <: Boolean](value: Channel[T]) = new Widget {
    val rendered = HTML.Container.Inline().rendered
    value.attach(cur => rendered.textContent = cur.toString)
    value.populate()
  }

  implicit def StringChannelToWidget[T <: String](value: Channel[T]) = new Widget {
    val rendered = HTML.Container.Inline().rendered
    value.attach(cur => rendered.textContent = cur.toString)
    value.populate()
  }

  implicit def IntChannelToWidget[T <: Int](value: Channel[T]) = new Widget {
    val rendered = HTML.Container.Inline().rendered
    value.attach(cur => rendered.textContent = cur.toString)
    value.populate()
  }

  implicit def DoubleChannelToWidget[T <: Double](value: Channel[T]) = new Widget {
    val rendered = HTML.Container.Inline().rendered
    value.attach(cur => rendered.textContent = cur.toString)
    value.populate()
  }

  implicit def WidgetChannelToWidget[T <: Widget](value: Channel[T]): Widget =
    HTML.Container.Inline().bind(value)

  implicit def OptWidgetChannelToWidget[T <: Option[Widget]](value: Channel[T]): Widget =
    HTML.Container.Inline().bindOpt(value)

  implicit def FunctionToChannel[T](f: T => Unit): Channel[T] = {
    val ch = Channel[T]()
    ch.attach(f)
    ch
  }
}