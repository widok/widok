package org.widok.bindings

import org.scalajs.dom

import pl.metastack.metarx._

import org.widok._

/**
 * Native widgets for Bootstrap 3 components
 */
package object Bootstrap {
  sealed trait TextStyle { val cssTag: String }
  object TextStyle {
    case object Small extends TextStyle { val cssTag = "small" }
    case object Mark extends TextStyle { val cssTag = "mark" }
    case object Left extends TextStyle { val cssTag = "text-left" }
    case object Right extends TextStyle { val cssTag = "text-right" }
    case object Center extends TextStyle { val cssTag = "text-center" }
    case object Justify extends TextStyle { val cssTag = "text-justify" }
    case object NoWrap extends TextStyle { val cssTag = "text-nowrap" }
    case object Lowercase extends TextStyle { val cssTag = "text-lowercase" }
    case object Uppercase extends TextStyle { val cssTag = "text-uppercase" }
    case object Capitalise extends TextStyle { val cssTag = "text-capitalize" }
    case object Muted extends TextStyle { val cssTag = "text-muted" }
    case object Primary extends TextStyle { val cssTag = "text-primary" }
    case object Success extends TextStyle { val cssTag = "text-success" }
    case object Info extends TextStyle { val cssTag = "text-info" }
    case object Warning extends TextStyle { val cssTag = "text-warning" }
    case object Danger extends TextStyle { val cssTag = "text-danger" }
  }

  sealed trait Role { val value: String }
  object Role {
    case object Search extends Role { val value = "search" }
  }

  def HorizontalForm(contents: View*) =
    HTML.Form(contents: _*)
      .css("form-horizontal")
      .attribute("role", "form")

  case class FormGroup(contents: View*) extends Widget[FormGroup] {
    val rendered = DOM.createElement("div", contents)
    css("form-group")

    def role(role: Role) = attribute("role", role.value)
    def size(value: Size) = css(s"form-group-${value.cssSuffix}")
  }

  case class InputGroup(contents: View*) extends Widget[InputGroup] {
    val rendered = DOM.createElement("div", contents)
    css("input-group")
  }

  object InputGroup {
    def Addon(contents: View*) = HTML.Container.Inline(contents: _*)
      .css("input-group-addon")
  }

  def ControlLabel(contents: View*) =
    HTML.Label(contents: _*)
      .css("control-label")

  sealed trait Style { val cssSuffix: String }
  object Style {
    /** Not supported by tables and alerts. */
    case object Default extends Style { val cssSuffix = "default" }

    /** Not supported by tables and alerts. */
    case object Primary extends Style { val cssSuffix = "primary" }

    case object Success extends Style { val cssSuffix = "success" }
    case object Info extends Style { val cssSuffix = "info" }
    case object Warning extends Style { val cssSuffix = "warning" }
    case object Danger extends Style { val cssSuffix = "danger" }
  }

  case class Label(contents: View*) extends Widget[Label] {
    val rendered = DOM.createElement("span", contents)
    css("label")

    def style(value: Style) = css(s"label-${value.cssSuffix}")
  }

  implicit class WidgetWithStyle[T](widget: Widget[T]) {
    def label(value: Style) = widget.css("label", s"label-${value.cssSuffix}")
    def textStyle(value: TextStyle) = widget.css(value.cssTag)
  }

  // TODO Improve design.
  def Fix(contents: View*) =
    HTML.Container.Generic(
      HTML.Container.Inline("Fix")
        .css("label", "label-warning"),
      HTML.Container.Inline(contents: _*)
    ).css("alert", "alert-danger")

  trait Input[T] extends Widget[T] { self: T =>
    def size(value: Size) = css(s"input-${value.cssSuffix}")
  }

  object Input {
    case class Text() extends HTML.Input.TextBase[Text] with Input[Text] {
      css("form-control")
    }

    case class Textarea() extends HTML.Input.TextareaBase[Textarea] with Input[Textarea] {
      css("form-control")
    }

    case class Password() extends HTML.Input.PasswordBase[Password] with Input[Password] {
      css("form-control")
    }

    case class Select() extends HTML.Input.SelectBase[Select] with Input[Select] {
      css("form-control")
    }

    case class Number() extends HTML.Input.NumberBase[Number] with Input[Number] {
      css("form-control")
    }
  }

  trait ButtonBase[T] extends Widget[T] { self: T =>
    def element: dom.html.Element

    val rendered = element
    css("btn", "btn-default")

    def size(value: Size) = css(s"btn-${value.cssSuffix}")
    def style(value: Style) = {
      cssState(false, "btn-default")
      css(s"btn-${value.cssSuffix}")
    }
    def block(state: Boolean) = cssState(state, "btn-block")
    def link(state: Boolean) = cssState(state, "btn-link")
  }

  case class Button(contents: View*) extends ButtonBase[Button] {
    def element = DOM.createElement("button", contents)
  }

  case class AnchorButton(contents: View*)
    extends ButtonBase[AnchorButton]
    with HTML.AnchorBase[AnchorButton]
  {
    def element = DOM.createElement("a", contents)
  }

  sealed trait Size { val cssSuffix: String }
  object Size {
    case object ExtraSmall extends Size { val cssSuffix = "xs" }
    case object Small extends Size { val cssSuffix = "sm" }
    case object Medium extends Size { val cssSuffix = "md" }
    case object Large extends Size { val cssSuffix = "lg" }
  }

  object Button {
    case class Group(buttons: Button*) extends Widget[Group] {
      val rendered = DOM.createElement("div", buttons)
      css("btn-group")
      attribute("role", "group")

      def size(value: Size) = css(s"btn-group-${value.cssSuffix}")

      // TODO Make NavigationBar.Branch() from above compatible with button groups.
    }

    case class Toolbar(groups: Group*) extends Widget[Toolbar] {
      val rendered = DOM.createElement("div", groups)
      css("btn-toolbar")
      attribute("role", "toolbar")
    }
  }

  object Checkbox {
    def apply(cb: HTML.Input.Checkbox, caption: View) =
      HTML.Container.Generic(
        HTML.Label(cb, caption)
      ).css("checkbox")

    def Inline(cb: HTML.Input.Checkbox, caption: View) =
      HTML.Label(cb, caption).css("checkbox-inline")
  }

  object Radio {
    def apply(radio: HTML.Input.Radio, caption: View) =
      HTML.Container.Generic(
        HTML.Label(radio, caption)
      ).css("radio")

    def Inline(radio: HTML.Input.Radio, caption: View) =
      HTML.Label(radio, caption).css("radio-inline")
  }

  def Footer(contents: View*) =
    HTML.Container.Generic(contents: _*)
      .css("footer")

  def Container(contents: View*) =
    HTML.Container.Generic(contents: _*)
      .css("container")

  def PageHeader(contents: View*) =
    HTML.Container.Generic(contents: _*)
      .css("page-header")

  def Lead(contents: View*) =
    HTML.Container.Generic(contents: _*)
      .css("lead")

  def PullRight(contents: View*) =
    HTML.Container.Inline(contents: _*)
      .css("pull-right")

  object Navigation {
    case class Tab(name: String)

    def renderTabs(tabs: Seq[Tab], currentTab: Channel[Tab]) = {
      val renderedTabs = tabs.map { tab =>
        val anchor = HTML.Anchor(tab.name).cursor(HTML.Cursor.Pointer)
        anchor.click.attach(_ => currentTab := tab)

        Bootstrap.Item(anchor).active(currentTab.map(_ == tab))
      }

      Bootstrap.Navigation.Tabs(renderedTabs: _*)
    }

    def Tabs(contents: Bootstrap.Item*) =
      HTML.List.Unordered(contents: _*)
        .css("nav", "nav-tabs")
        .attribute("role", "tablist")

    def Pills(contents: Bootstrap.Item*) =
      HTML.List.Unordered(contents: _*)
        .css("nav", "nav-pills")
        .attribute("role", "tablist")
  }

  case class Item(contents: View*) extends Widget.List.Item[Item] {
    val rendered = DOM.createElement("li", contents)
    def active(active: ReadChannel[Boolean]) = cssState(active, "active")
  }

  case class NavigationBar(contents: View*) extends Widget[NavigationBar] {
    val chPosition: Var[(NavigationBar.Position, Boolean)] =
      Var((NavigationBar.Position.Top, true))
    val rendered = HTML.Navigation(contents: _*).css(
      chPosition.map { case (position, fixed) =>
        Set("navbar", "navbar-default", position.cssTag(fixed))
      }
    ).attribute("role", "navigation").rendered

    def position(position: NavigationBar.Position, fixed: Boolean = true) = {
      chPosition := ((position, fixed))
      this
    }
  }

  object NavigationBar {
    sealed trait Position {
      def cssTag(fixed: Boolean): String
    }

    object Position {
      case object Top extends Position {
        def cssTag(fixed: Boolean) = {
          val scrollingBehaviour = if (fixed) "fixed" else "static"
          s"navbar-$scrollingBehaviour-top"
        }
      }

      case object Bottom extends Position {
        def cssTag(fixed: Boolean) = {
          val scrollingBehaviour = if (fixed) "fixed" else "static"
          s"navbar-$scrollingBehaviour-bottom"
        }
      }
    }

    private val collapseClosed = Var(true)

    def Toggle() =
      HTML.Button(
        HTML.Container.Inline().css("icon-bar"),
        HTML.Container.Inline().css("icon-bar"),
        HTML.Container.Inline().css("icon-bar")
      ).css("navbar-toggle", "collapsed")
        .attribute("type", "button")
        .onClick(_ => collapseClosed.update(!_))

    def Header(contents: View*) =
      HTML.Container.Generic(contents: _*)
        .css("navbar-header")

    def Brand(contents: View*) =
      HTML.Anchor(contents: _*)
        .css("navbar-brand")

    def Collapse(contents: View*) =
      HTML.Container.Generic(contents: _*)
        .css("navbar-collapse")
        .cssState(collapseClosed, "collapse")

    def Branch(contentsCaption: Widget[_]*)
              (contents: Widget.List.Item[_]*): Bootstrap.Item = {
      val open = Var(false)

      Bootstrap.Item(
        HTML.Anchor(
          HTML.Container.Inline(contentsCaption: _*),
          HTML.Container.Inline().css("caret")
        ).css("dropdown-toggle")

        , HTML.List.Unordered(contents: _*)
          .css("dropdown-menu")
          .attribute("role", "menu")
      ).css("dropdown")
        .cssState(open, "open")
        .onClick(_ => open.update(!_))
    }

    def Elements(contents: Bootstrap.Item*) =
      HTML.List.Unordered(contents: _*)
        .css("nav", "navbar-nav")

    def Form(contents: View*) =
      HTML.Container.Generic(contents: _*)
        .css("navbar-form")

    def Left(contents: View*) =
      HTML.Container.Generic(contents: _*)
        .css("navbar-left")

    def Right(contents: View*) =
      HTML.Container.Generic(contents: _*)
        .css("navbar-right")

    def Text(contents: View*) =
      HTML.Container.Generic(contents: _*)
        .css("navbar-text")

    def Navigation(contents: View*) =
      HTML.Container.Generic(contents: _*)
        .css("nav", "navbar-nav")
  }

  case class Alert(contents: View*) extends Widget[Alert] {
    val rendered = DOM.createElement("div", contents)
    css("alert")
    attribute("role", "alert")

    def style(value: Style) = {
      css(s"alert-${value.cssSuffix}")
    }
  }

  case class Panel(contents: View*) extends Widget[Panel] {
    val rendered = DOM.createElement("div", contents)
    css("panel")
    css("panel-default")

    def style(value: Style) = {
      cssState(false, "panel-default")
      css(s"panel-${value.cssSuffix}")
    }
  }

  object Panel {
    def Body(contents: View*) =
      HTML.Container.Generic(contents: _*)
        .css("panel-body")

    def Heading(contents: View*) =
      HTML.Container.Generic(contents: _*)
        .css("panel-heading")

    def Title1(contents: View*) =
      HTML.Heading.Level1(contents: _*)
        .css("panel-title")

    def Title2(contents: View*) =
      HTML.Heading.Level2(contents: _*)
        .css("panel-title")

    def Title3(contents: View*) =
      HTML.Heading.Level3(contents: _*)
        .css("panel-title")

    def Title4(contents: View*) =
      HTML.Heading.Level4(contents: _*)
        .css("panel-title")

    def Title5(contents: View*) =
      HTML.Heading.Level5(contents: _*)
        .css("panel-title")

    def Title6(contents: View*) =
      HTML.Heading.Level6(contents: _*)
        .css("panel-title")

    def Footer(contents: View*) =
      HTML.Container.Generic(contents: _*)
        .css("panel-footer")
  }

  case class Pagination(contents: Pagination.Item*) extends Widget.List[Pagination] {
    val rendered =
      HTML.Navigation(
        HTML.List.Unordered(
          contents: _*
        ).css("pagination")
      ).rendered
  }

  object Pagination {
    case class Item(contents: View*) extends Widget.List.Item[Item] {
      val rendered = DOM.createElement("li", contents)

      def size(value: Size) = css(s"pagination-${value.cssSuffix}")
      def active(state: Boolean) = cssState(state, "active")
      def disabled(state: Boolean) = cssState(state, "disabled")
    }
  }

  case class ListGroup(contents: Widget.List.Item[_]*) extends Widget.List[ListGroup] {
    val rendered = HTML.Container.Generic(contents: _*)
      .css("list-group")
      .rendered
  }

  object ListGroup {
    case class Item(contents: View*) extends Widget.List.Item[Item] {
      val widget = HTML.Container.Inline(contents: _*)
        .css("list-group-item")
      val rendered = widget.rendered

      def active(state: Boolean) = cssState(state, "active")

      /** Needed in conjunction with PullRight(). */
      def clearfix(state: Boolean) = cssState(state, "clearfix")
    }

    def ItemHeading1(contents: View*) =
      HTML.Heading.Level1(contents: _*)
        .css("list-group-item-heading")

    def ItemHeading2(contents: View*) =
      HTML.Heading.Level2(contents: _*)
        .css("list-group-item-heading")

    def ItemHeading3(contents: View*) =
      HTML.Heading.Level3(contents: _*)
        .css("list-group-item-heading")

    def ItemHeading4(contents: View*) =
      HTML.Heading.Level4(contents: _*)
        .css("list-group-item-heading")

    def ItemHeading5(contents: View*) =
      HTML.Heading.Level5(contents: _*)
        .css("list-group-item-heading")

    def ItemHeading6(contents: View*) =
      HTML.Heading.Level6(contents: _*)
        .css("list-group-item-heading")

    def ItemText(contents: View*) =
      HTML.Paragraph(contents: _*)
        .css("list-group-item-text")
  }

  object Grid {
    case class Row(contents: Widget.List.Item[_]*) extends Widget.List[Row] {
      val rendered = DOM.createElement("div", contents)
      css("row")
    }

    case class Column(contents: View*) extends Widget.List.Item[Column] {
      val rendered = DOM.createElement("div", contents)

      def column(size: Size, level: Int) = css(s"col-${size.cssSuffix}-$level")
      def offset(size: Size, level: Int) = css(s"col-${size.cssSuffix}-offset-$level")
      def pull(size: Size, level: Int) = css(s"col-${size.cssSuffix}-pull-$level")
      def push(size: Size, level: Int) = css(s"col-${size.cssSuffix}-push-$level")
    }
  }

  case class Modal(contents: View*) extends Widget[Modal] {
    val rendered = DOM.createElement("div", contents)
    css("modal")

    def fade(state: Boolean) = cssState(state, "fade")
  }

  object Modal {
    def Backdrop() =
      HTML.Container.Generic().css("modal-backdrop", "in")

    def Close(dismiss: () => Any) =
      HTML.Button(HTML.Container.Inline("×"))
        .css("close").onClick { _ => dismiss(); () }

    def Title(contents: View*) =
      HTML.Heading.Level4(contents: _*).css("modal-title")

    def Dialog(contents: View*) =
      HTML.Container.Generic(contents: _*).css("modal-dialog")

    trait ContentElement extends Widget[ContentElement]
    def Content(contents: ContentElement*) =
      HTML.Container.Generic(contents: _*).css("modal-content")

    case class Header(contents: View*) extends ContentElement {
      val rendered = DOM.createElement("div", contents)
      css("modal-header")
    }

    case class Body(contents: View*) extends ContentElement {
      val rendered = DOM.createElement("div", contents)
      css("modal-body")
    }

    case class Footer(contents: View*) extends ContentElement {
      val rendered = DOM.createElement("div", contents)
      css("modal-footer")
    }
  }

  case class ProgressBar(contents: View*) extends Widget[ProgressBar] {
    val pb = HTML.Container.Generic(
      contents: _*
    ).css("progress-bar")
     .attribute("role", "progressbar")

    val rendered = DOM.createElement("div", Seq(pb))
    css("progress")

    def style(value: ReadChannel[Style]) = {
      pb.css(value.map(x => Set("progress-bar", "progress-bar-" + x.cssSuffix)))
      this
    }

    def progress(percentage: ReadChannel[Double]) = {
      pb.width(percentage.map(Length.Percentage(_).asInstanceOf[Length]))
      this
    }
  }

  case class Media(contents: View*) extends Widget[Media] {
    val rendered = DOM.createElement("div", contents)
    css("media")
  }

  object Media {
    def Left(contents: View*) =
      HTML.Container.Generic(contents: _*)
        .css("media-left")

    def List(contents: Bootstrap.Media.Item*) =
      HTML.List.Unordered(contents: _*)
        .css("media-left")

    case class Item(contents: View*) extends Widget.List.Item[Item] {
      val rendered = DOM.createElement("li", contents)
      css("media")
    }

    def Body(contents: View*) =
      HTML.Container.Generic(contents: _*)
        .css("media-body")

    def Object(source: String) =
      HTML.Image(source)
        .css("media-object")

    def Heading(contents: View*) =
      HTML.Heading.Level4(contents: _*)
        .css("media-heading")
  }

  sealed trait Shape { val cssTag: String }
  object Shape {
    case object Rounded   extends Shape { val cssTag = "img-rounded"   }
    case object Circle    extends Shape { val cssTag = "img-circle"    }
    case object Thumbnail extends Shape { val cssTag = "img-thumbnail" }
  }

  case class Image(source: String) extends Widget[Image] {
    val rendered = DOM.createElement("img")
    rendered.setAttribute("src", source)

    def responsive(state: Boolean) = cssState(state, "img-responsive")
    def shape(value: Shape) = css(value.cssTag)
  }

  case class Breadcrumb(contents: Bootstrap.Item*) extends Widget.List[Breadcrumb] {
    val rendered = DOM.createElement("ol", contents)
    css("breadcrumb")
  }

  case class Table(contents: View*) extends Widget[Table] {
    val rendered = DOM.createElement("table", contents)
    css("table")

    def striped(state: Boolean) = cssState(state, "table-striped")
    def bordered(state: Boolean) = cssState(state, "table-bordered")
    def hover(state: Boolean) = cssState(state, "table-hover")
    def condensed(state: Boolean) = cssState(state, "table-condensed")
  }

  object Table {
    def Responsive(table: Table) =
      HTML.Container.Generic(table)
        .css("table-responsive")

    case class Row(contents: View*) extends Widget.List.Item[Row] {
      val rendered = DOM.createElement("tr", contents)

      def active(state: Boolean) = cssState(state, "active")
      def style(style: Style) = css(style.cssSuffix)
    }

    case class Column(contents: View*) extends Widget[Column] {
      val rendered = DOM.createElement("td", contents)

      def active(state: Boolean) = cssState(state, "active")
      def style(style: Style) = css(style.cssSuffix)
    }
  }

  case class Validation[T](ch: ReadChannel[T], f: T => Option[String])

  case class Validator(validations: Validation[_]*) {
    val dirty = Var(false)
    val invalid = Dict[ReadChannel[_], String]()
    val valid = invalid.isEmpty.cache(true)

    validations.foreach { case Validation(ch, f) =>
      ch.attach { input =>
        f(input) match {
          case Some(err) => invalid.insertOrUpdate(ch, err)
          case None => invalid.removeIfExists(ch)
        }
      }
    }

    val validators = validations
      .foldLeft(Map.empty[ReadChannel[_], ReadChannel[Option[String]]])
    { case (acc, Validation(ch, _)) =>
      acc + (ch -> dirty.flatMap(
        if (_) invalid.value(ch)
        else Var(None)
      ))
    }

    val maySubmit = dirty.flatMap {
      if (_) valid else Var(true)
    }

    def check(): Boolean = {
      dirty := true
      valid.get
    }

    def validate[T](x: Widget[T], field: ReadChannel[_]) =
      x.cssState(validators(field).map(_.nonEmpty), "has-error")

    def errors(field: ReadChannel[_]) =
      HTML.Container.Inline(
        validators(field).map(_.getOrElse(""))
      ).cssState(validators(field).map(_.nonEmpty), "help-block", "with-errors")
  }

  implicit class ValidateWidget[T](widget: Widget[T]) {
    def validate(field: ReadChannel[_])(implicit v: Validator) = v.validate(widget, field)
  }
}
