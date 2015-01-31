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
    HTML.Container.Inline = HTML.Container.Inline().subscribe(value)

  implicit def IntChannelToWidget[T <: Int](value: ReadChannel[T]):
    HTML.Container.Inline = HTML.Container.Inline().subscribe(value.map(_.toString))

  implicit def DoubleChannelToWidget[T <: Double](value: ReadChannel[T]):
    HTML.Container.Inline = HTML.Container.Inline().subscribe(value.map(_.toString))

  implicit def BooleanChannelToWidget[T <: Boolean](value: ReadChannel[T]):
    HTML.Container.Inline = HTML.Container.Inline().subscribe(value.map(_.toString))

  implicit def WidgetChannelToWidget[T <: Widget[_]](value: ReadChannel[T]):
    HTML.Container.Inline = HTML.Container.Inline().widget(value)

  implicit def OptWidgetChannelToWidget[T <: Option[Widget[_]]]
    (value: ReadChannel[T]): HTML.Container.Inline =
      HTML.Container.Inline().optWidget(value)

  implicit def WidgetAggregateToWidget[T <: Widget[_]](agg: Aggregate[T]):
    /* TODO Why do we have to cast? */
    HTML.List.Items = HTML.List.Items(agg.asInstanceOf[Aggregate[Widget[_]]])

  implicit def WidgetSeqToWidget[T <: Widget[_]](agg: Seq[T]): Inline =
    Inline(agg: _*)

  implicit def StringBufferToWidget[T <: String](buf: ReadBuffer[T]):
    HTML.List.Items =
      /* TODO This could be optimised by creating fewer DOM nodes. */
      HTML.List.Items(buf
        .map(x => HTML.Text(x.get)))

  implicit def InstantiatedRouteToString(route: InstantiatedRoute): String =
    route.uri()

  implicit def FunctionToWriteChannel[T](f: T => Unit): WriteChannel[T] = {
    val ch = Channel[T]()
    ch.attach(f)
    ch
  }

  /** Short aliases for HTML tags
    * See also http://stackoverflow.com/questions/21831497/type-aliasing-a-case-class-in-scala-2-10
    */
  object html {
    val section = bindings.HTML.Section
    val header = bindings.HTML.Header
    val footer = bindings.HTML.Footer
    val nav = bindings.HTML.Navigation

    val h1 = bindings.HTML.Heading.Level1
    val h2 = bindings.HTML.Heading.Level2
    val h3 = bindings.HTML.Heading.Level3
    val h4 = bindings.HTML.Heading.Level4
    val h5 = bindings.HTML.Heading.Level5
    val h6 = bindings.HTML.Heading.Level6

    val p = bindings.HTML.Paragraph
    val b = bindings.HTML.Text.Bold
    val i = bindings.HTML.Text.Italic
    val strong = bindings.HTML.Text.Bold
    val small = bindings.HTML.Text.Small

    val br = bindings.HTML.LineBreak
    val hr = bindings.HTML.HorizontalLine

    val div = bindings.HTML.Container.Generic
    val span = bindings.HTML.Container.Inline

    val form = bindings.HTML.Form
    val button = bindings.HTML.Button
    val label = bindings.HTML.Label
    val a = bindings.HTML.Anchor
    val img = bindings.HTML.Image

    val checkbox = bindings.HTML.Input.Checkbox
    val file = bindings.HTML.Input.File
    val select = bindings.HTML.Input.Select
    val text = bindings.HTML.Input.Text
    val password = bindings.HTML.Input.Password
    val option = bindings.HTML.Input.Select.Option

    val ul = bindings.HTML.List.Unordered
    val ol = bindings.HTML.List.Ordered
    val li = bindings.HTML.List.Item

    val table = bindings.HTML.Table
    val thead = bindings.HTML.Table.Head
    val th = bindings.HTML.Table.HeadColumn
    val tbody = bindings.HTML.Table.Body
    val tr = bindings.HTML.Table.Row
    val td = bindings.HTML.Table.Column

    val cursor = bindings.HTML.Cursor
  }
}