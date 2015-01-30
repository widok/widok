package org.widok.bindings

import org.widok._

/**
 * Native widgets for Bootstrap 3 components
 */
package object Bootstrap {
  object TextContainer {
    trait Style { val cssTag: String }
    object Style {
      case object Small extends Style { val cssTag = "small" }
      case object Mark extends Style { val cssTag = "mark" }
      case object Left extends Style { val cssTag = "text-left" }
      case object Right extends Style { val cssTag = "text-right" }
      case object Center extends Style { val cssTag = "text-center" }
      case object Justify extends Style { val cssTag = "text-justify" }
      case object NoWrap extends Style { val cssTag = "text-nowrap" }
      case object Lowercase extends Style { val cssTag = "text-lowercase" }
      case object Uppercase extends Style { val cssTag = "text-uppercase" }
      case object Capitalise extends Style { val cssTag = "text-capitalize" }
      case object Muted extends Style { val cssTag = "text-muted" }
      case object Primary extends Style { val cssTag = "text-primary" }
      case object Success extends Style { val cssTag = "text-success" }
      case object Info extends Style { val cssTag = "text-info" }
      case object Warning extends Style { val cssTag = "text-warning" }
      case object Danger extends Style { val cssTag = "text-danger" }
    }

    def apply(styles: Style*)(contents: View*) =
      HTML.Container.Generic(contents: _*)
        .css(styles.map(_.cssTag): _*)
  }

  trait Role { val value: String }
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
  }

  def InputGroup(contents: View*) =
    HTML.Container.Generic(contents: _*)
      .css("input-group")

  def ControlLabel(contents: View*) =
    HTML.Label(contents: _*)
      .css("control-label")

  object Label {
    trait Style { val cssTag: String }
    object Style {
      case object Default extends Style { val cssTag = "label-default" }
      case object Primary extends Style { val cssTag = "label-primary" }
      case object Success extends Style { val cssTag = "label-success" }
      case object Info extends Style { val cssTag = "label-info" }
      case object Warning extends Style { val cssTag = "label-warning" }
      case object Danger extends Style { val cssTag = "label-danger" }
    }

    def apply(style: ReadChannel[Label.Style])(contents: View*) =
      HTML.Container.Generic(contents: _*)
        .css("label")
        .cssCh(style.map(_.cssTag))
  }

  // TODO Improve design.
  def Fix(contents: View*) =
    HTML.Container.Generic(
      HTML.Container.Inline("Fix")
        .css("label label-warning"),
      HTML.Container.Inline(contents: _*)
    ).css("alert alert-danger")

  object Input {
    def Text(): HTML.Input.Text =
      HTML.Input.Text()
        .css("form-control")

    def Password(): HTML.Input.Password =
      HTML.Input.Password()
        .css("form-control")

    def Select(options: Seq[String], selected: Int = -1): HTML.Input.Select =
      HTML.Input.Select(options, selected)
        .css("form-control")
  }

  case class Button(contents: View*) extends Widget[Button] {
    val rendered = DOM.createElement("button", contents)
    css("btn btn-default")

    def size(size: Size) = css("btn-" + size.cssSuffix)
    def style(style: Button.Style) = {
      css(false, "btn-default")
      css(style.cssTag)
    }
    def block(state: Boolean) = css(state, "btn-block")
  }

  trait Size { val cssSuffix: String }
  object Size {
    case object ExtraSmall extends Size { val cssSuffix = "xs" }
    case object Small extends Size { val cssSuffix = "sm" }
    case object Medium extends Size { val cssSuffix = "md" }
    case object Large extends Size { val cssSuffix = "lg" }
  }

  object Button {
    trait Style { val cssTag: String }
    object Style {
      case object Default extends Style { val cssTag = "btn-default" }
      case object Primary extends Style { val cssTag = "btn-primary" }
      case object Success extends Style { val cssTag = "btn-success" }
      case object Info extends Style { val cssTag = "btn-info" }
      case object Warning extends Style { val cssTag = "btn-warning" }
      case object Danger extends Style { val cssTag = "btn-danger" }
      case object Link extends Style { val cssTag = "btn-link" }
    }

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

  def MutedText(contents: View*) =
    HTML.Paragraph(contents: _*)
      .css("text-muted")

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
        .css("nav nav-tabs")
        .attribute("role", "tablist")

    def Pills(contents: Bootstrap.Item*) =
      HTML.List.Unordered(contents: _*)
        .css("nav nav-pills")
        .attribute("role", "tablist")
  }

  case class Item(contents: View*) extends Widget.List.Item[Item] {
    val rendered = DOM.createElement("li", contents)
    def active(active: ReadChannel[Boolean]) = cssCh(active, "active")
  }

  object NavigationBar {
    trait Position {
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

    def apply(position: NavigationBar.Position = NavigationBar.Position.Top, fixed: Boolean = true)(contents: View*) =
      HTML.Navigation(contents: _*)
        .css("navbar", "navbar-default", position.cssTag(fixed))
        .attribute("role", "navigation")

    def Toggle() =
      HTML.Button(
        HTML.Container.Inline().css("icon-bar"),
        HTML.Container.Inline().css("icon-bar"),
        HTML.Container.Inline().css("icon-bar")
      ).css("navbar-toggle collapsed")
        .attribute("type", "button")

    def Header(contents: View*) =
      HTML.Container.Generic(contents: _*)
        .css("navbar-header")

    def Brand(contents: View*) =
      HTML.Anchor(contents: _*)
        .css("navbar-brand")

    def Collapse(contents: View*) =
      HTML.Container.Generic(contents: _*)
        .css("collapse", "navbar-collapse")

    def Branch(contentsCaption: Widget[_]*)
              (contents: Bootstrap.Item*): Bootstrap.Item = {
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
        .cssCh(open, "open")
        .onClick(_ => open := !open.get)
    }

    def Elements(contents: Bootstrap.Item*) =
      HTML.List.Unordered(contents: _*)
        .css("nav", "navbar-nav")

    def Form(contents: View*) =
      HTML.Container.Generic(contents: _*)
        .css("navbar-form")

    def Right(contents: View*) =
      HTML.Container.Generic(contents: _*)
        .css("navbar-right")

    def Navigation(contents: View*) =
      HTML.Container.Generic(contents: _*)
        .css("nav", "navbar-nav")
  }

  // TODO Find a better solution.
  case class Checkbox(contents: View*) extends Widget[Checkbox] {
    val checkbox = HTML.Input.Checkbox()

    val rendered =
      HTML.Container.Generic(
        ControlLabel(
          checkbox,
          HTML.Container.Inline(contents: _*)
        )
      ).css("checkbox")
        .rendered
  }

  trait AlertType { val cssTag: String }
  object AlertType {
    case object Success extends AlertType { val cssTag = "alert-success" }
    case object Info extends AlertType { val cssTag = "alert-info" }
    case object Warning extends AlertType { val cssTag = "alert-warning" }
    case object Danger extends AlertType { val cssTag = "alert-danger" }
  }

  def Alert(alertType: AlertType)(contents: View*) =
    HTML.Container.Generic(contents: _*)
      .css(s"alert ${alertType.cssTag}")
      .attribute("role", "alert")

  def Panel(contents: View*) =
    HTML.Container.Generic(
      HTML.Container.Generic(
        contents: _*
      ) .css("panel-body")
    ) .css("panel panel-default")

  object ListGroup {
    case class Group(contents: Widget.List.Item[_]*) extends Widget.List[Group, Widget.List.Item[_]] {
      val rendered = HTML.Container.Generic(contents: _*)
        .css("list-group")
        .rendered
    }

    case class PageItem(contents: View*) extends Widget.List.Item[PageItem] {
      val widget = HTML.Anchor(contents: _*)
        .css("list-group-item")
      val rendered = widget.rendered

      def url(value: String) = { widget.url(value); this }
      def active(ch: Channel[Boolean]) = cssCh(ch, "active")
    }

    // clearfix is needed in conjunction with PullRight()
    case class Item(contents: View*) extends Widget.List.Item[Item] {
      val widget = HTML.Container.Generic(contents: _*)
        .css("list-group-item", "clearfix")
      val rendered = widget.rendered

      def active(ch: Channel[Boolean]) = cssCh(ch, "active")
    }

    def ItemHeading(contents: View*) =
      HTML.Heading.Level4(contents: _*)
        .css("list-group-item-heading")

    def ItemText(contents: View*) =
      HTML.Paragraph(contents: _*)
        .css("list-group-item-text")
  }

  object Grid {
    case class Row(contents: Column*) extends Widget.List[Row, Column] {
      val rendered = DOM.createElement("div", contents)
      css("row")
    }

    case class Column(contents: View*) extends Widget[Column] {
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

    def fade(state: Boolean) = css(state, "fade")
  }

  object Modal {
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

  case class Breadcrumb(contents: Bootstrap.Item*) extends Widget.List[Breadcrumb, Bootstrap.Item] {
    val rendered = DOM.createElement("ol", contents)
    css("breadcrumb")
  }
}
