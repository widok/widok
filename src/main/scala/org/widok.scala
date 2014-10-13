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

  implicit def WidgetToSeq[T <: Widget[T]](view: Widget[T]) = Seq(view)

  implicit def StringToWidget(value: String) = HTML.Text(value)

  implicit def StringChannelToWidget[T <: String](value: Channel[T]) =
    HTML.Container.Inline().bindString(value)

  implicit def IntChannelToWidget[T <: Int](value: Channel[T]) =
    HTML.Container.Inline().bindInt(value)

  implicit def DoubleChannelToWidget[T <: Double](value: Channel[T]) =
    HTML.Container.Inline().bindDouble(value)

  implicit def BooleanChannelToWidget[T <: Boolean](value: Channel[T]) =
    HTML.Container.Inline().bindBoolean(value)

  implicit def WidgetChannelToWidget[T <: Widget[_]](value: Channel[T]) =
    HTML.Container.Inline().bindWidget(value)

  implicit def OptWidgetChannelToWidget[T <: Option[Widget[_]]](value: Channel[T]) =
    HTML.Container.Inline().bindOptWidget(value)

  implicit def InstantiatedRouteToString(route: InstantiatedRoute): String = route.uri()

  implicit def FunctionToChannel[T](f: T => Unit): Channel[T] = {
    val ch = Channel[T]()
    ch.attach(f)
    ch
  }
}