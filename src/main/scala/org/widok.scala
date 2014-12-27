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

  implicit def StringToWidget(value: String): HTML.Text = HTML.Text(value)

  implicit def StringChannelToWidget[T <: String](value: ReadChannel[T]):
    HTML.Container.Inline = HTML.Container.Inline().bindString(value)

  implicit def IntChannelToWidget[T <: Int](value: ReadChannel[T]):
    HTML.Container.Inline = HTML.Container.Inline().bindInt(value)

  implicit def DoubleChannelToWidget[T <: Double](value: ReadChannel[T]):
    HTML.Container.Inline = HTML.Container.Inline().bindDouble(value)

  implicit def BooleanChannelToWidget[T <: Boolean](value: ReadChannel[T]):
    HTML.Container.Inline = HTML.Container.Inline().bindBoolean(value)

  implicit def WidgetChannelToWidget[T <: Widget[_]](value: ReadChannel[T]):
    HTML.Container.Inline = HTML.Container.Inline().bindWidget(value)

  implicit def WidgetAggregateToWidget[T <: Widget[_]](agg: Aggregate[T]):
    /* TODO Why do we have to cast? */
    HTML.List.Items = HTML.List.Items(agg.asInstanceOf[Aggregate[Widget[_]]])

  implicit def StringBufferToWidget[T <: String](buf: ReadBuffer[T]):
    HTML.List.Items =
      /* TODO This could be optimised by creating fewer DOM nodes. */
      HTML.List.Items(buf
        .map(x => HTML.Text(x.get)))

  implicit def OptWidgetChannelToWidget[T <: Option[Widget[_]]]
    (value: ReadChannel[T]): HTML.Container.Inline =
      HTML.Container.Inline().bindOptWidget(value)

  implicit def InstantiatedRouteToString(route: InstantiatedRoute): String =
    route.uri()

  implicit def FunctionToWriteChannel[T](f: T => Unit): WriteChannel[T] = {
    val ch = Channel[T]()
    ch.attach(f)
    ch
  }
}