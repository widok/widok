package org.widok.bindings

import org.widok._

/**
 * Native widgets for Bootstrap 3 components
 */
object Bootstrap {
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

    def apply(styles: Style*)(contents: Widget[_]*) =
      HTML.Container.Generic(contents: _*)
        .css(styles.map(_.cssTag): _*)
  }

  // TODO Add missing ones.
  trait Glyphicon { val cssTag: String }
  object Glyphicon {
    case object None extends Glyphicon { val cssTag = "" }
    case object Ok extends Glyphicon { val cssTag = "glyphicon-ok" }
    case object Star extends Glyphicon { val cssTag = "glyphicon-star" }
    case object Search extends Glyphicon { val cssTag = "glyphicon-search" }
    case object ArrowLeft extends Glyphicon { val cssTag = "glyphicon-arrow-left" }
    case object ArrowRight extends Glyphicon { val cssTag = "glyphicon-arrow-right" }
    case object Bookmark extends Glyphicon { val cssTag = "glyphicon-bookmark" }
    case object Fire extends Glyphicon { val cssTag = "glyphicon-fire" }
    case object Time extends Glyphicon { val cssTag = "glyphicon-time" }
    case object Dashboard extends Glyphicon { val cssTag = "glyphicon-dashboard" }
    case object Book extends Glyphicon { val cssTag = "glyphicon-book" }
    case object Statistics extends Glyphicon { val cssTag = "glyphicon-stats" }
    case object Open extends Glyphicon { val cssTag = "glyphicon-open" }
    case object Plus extends Glyphicon { val cssTag = "glyphicon-plus" }
    case object Trash extends Glyphicon { val cssTag = "glyphicon-trash" }
    case object Left extends Glyphicon { val cssTag = "glyphicon-left" }
    case object Right extends Glyphicon { val cssTag = "glyphicon-right" }
    case object Font extends Glyphicon { val cssTag = "glyphicon-font" }
    case object Bold extends Glyphicon { val cssTag = "glyphicon-bold" }
    case object Italic extends Glyphicon { val cssTag = "glyphicon-italic" }
    case object Wrench extends Glyphicon { val cssTag = "glyphicon-wrench" }
    case object Comment extends Glyphicon { val cssTag = "glyphicon-comment" }
    case object User extends Glyphicon { val cssTag = "glyphicon-user" }
    case object Off extends Glyphicon { val cssTag = "glyphicon-off" }
    case object Pause extends Glyphicon { val cssTag = "glyphicon-pause" }
    case object Stop extends Glyphicon { val cssTag = "glyphicon-stop" }
  }

  trait Role { val value: String }
  object Role {
    case object None extends Role { val value = "" }
    case object Search extends Role { val value = "search" }
  }

  def Glyphicon(glyphicon: Glyphicon, caption: String = "") =
    HTML.Container.Inline()
      .css("glyphicon", glyphicon.cssTag)
      .attribute("title", caption)

  def HorizontalForm(contents: Widget[_]*) =
    HTML.Form(contents: _*)
      .css("form-horizontal")
      .attribute("role", "form")

  def FormGroup(role: Role = Role.None)(contents: Widget[_]*) = {
    val res = HTML.Container.Generic(contents: _*)
      .css("form-group")

    if (role == Role.None) res
    else res.attribute("role", role.value)
  }

  def InputGroup(contents: Widget[_]*) =
    HTML.Container.Generic(contents: _*)
      .css("input-group")

  def ControlLabel(contents: Widget[_]*) =
    HTML.Label(contents: _*)
      .css("control-label")

  object Label {
    trait Style
    object Style {
      case object Default extends Style { override def toString = "label-default" }
      case object Primary extends Style { override def toString = "label-primary" }
      case object Success extends Style { override def toString = "label-success" }
      case object Info extends Style { override def toString = "label-info" }
      case object Warning extends Style { override def toString = "label-warning" }
      case object Danger extends Style { override def toString = "label-danger" }
    }

    def apply(style: Channel[Label.Style])(contents: Widget[_]*) =
      HTML.Container.Generic(contents: _*)
        .css("label")
        .cssCh(style.map(_.toString))
  }

  // TODO Improve design.
  def Fix(contents: Widget[_]*) =
    HTML.Container.Generic(
      HTML.Container.Inline("Fix")
        .css("label label-warning"),
      HTML.Container.Inline(contents: _*)
    ).css("alert alert-danger")

  object Input {
    def Text() =
      HTML.Input.Text()
        .css("form-control")
        .asInstanceOf[HTML.Input.Text] // Workaround

    def Select(options: Seq[String], selected: Int = -1) =
      HTML.Input.Select(options, selected)
        .css("form-control")
        .asInstanceOf[HTML.Input.Select] // Workaround
  }

  object Button {
    trait Size { val cssTag: String }
    object Size {
      case object Normal extends Size { val cssTag = "" }
      case object ExtraSmall extends Size { val cssTag = "btn-xs" }
    }

    // For better usability all buttons should have icons. Therefore
    // None is not the default value.
    def apply(icon: Glyphicon, size: Size = Size.Normal)(contents: Widget[_]*) = {
      val btn =
        if (icon == Glyphicon.None) HTML.Button(contents: _*)
        else HTML.Button(Glyphicon(icon) :: contents.toList: _*)

      btn.css("btn btn-default")
    }
  }

  def Footer(contents: Widget[_]*) =
    HTML.Container.Generic(contents: _*)
      .css("footer")

  def Container(contents: Widget[_]*) =
    HTML.Container.Generic(contents: _*)
      .css("container")

  def PageHeader(contents: Widget[_]*) =
    HTML.Container.Generic(contents: _*)
      .css("page-header")

  def Lead(contents: Widget[_]*) =
    HTML.Container.Generic(contents: _*)
      .css("lead")

  def PullRight(contents: Widget[_]*) =
    HTML.Container.Inline(contents: _*)
      .css("pull-right")

  def MutedText(contents: Widget[_]*) =
    HTML.Paragraph(contents: _*)
      .css("text-muted")

  object Navigation {
    case class Tab(name: String)

    def renderTabs(tabs: Seq[Tab], currentTab: Channel[Tab]) = {
      val renderedTabs = tabs.map(tab =>
        Bootstrap.Navigation.Item(currentTab.map(_ == tab))(
          HTML.Anchor(tab.name)
            .bind((_: Unit) => currentTab := tab)
            .cursor(HTML.Cursor.Pointer)
        )
      )
      Bootstrap.Navigation.Tabs(renderedTabs: _*)
    }

    def Tabs(contents: HTML.List.Item*) =
      HTML.List.Unordered(contents: _*)
        .css("nav nav-tabs")
        .attribute("role", "tablist")

    def Pills(contents: HTML.List.Item*) =
      HTML.List.Unordered(contents: _*)
        .css("nav nav-pills")
        .attribute("role", "tablist")

    def Item(active: Channel[Boolean] = Channel())(contents: Widget[_]*) =
      HTML.List.Item(contents: _*)
        .cssCh(active, "active")
        .asInstanceOf[HTML.List.Item] // TODO Workaround
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

    def apply(position: NavigationBar.Position = NavigationBar.Position.Top, fixed: Boolean = true)(contents: Widget[_]*) =
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

    def Header(contents: Widget[_]*) =
      HTML.Container.Generic(contents: _*)
        .css("navbar-header")

    def Brand(contents: Widget[_]*) =
      HTML.Anchor(contents: _*)
        .css("navbar-brand")

    def Collapse(contents: Widget[_]*) =
      HTML.Container.Generic(contents: _*)
        .css("collapse", "navbar-collapse")

    def Leaf(url: String, active: Channel[Boolean] = Channel())(contents: Widget[_]*) =
      HTML.List.Item(
        HTML.Anchor(contents: _*)
          .url(url)
      ).cssCh(active, "active")
       .asInstanceOf[HTML.List.Item] // TODO Workaround

    def Branch(contentsCaption: Widget[_]*)(contents: HTML.List.Item*) =
      HTML.List.Item(
        HTML.Anchor(
          HTML.Container.Inline(contentsCaption: _*),
          HTML.Container.Inline().css("caret")
        ).css("dropdown-toggle"),
        HTML.List.Unordered(contents: _*)
          .css("dropdown-menu")
          .attribute("role", "menu")
      ).css("dropdown")
       .asInstanceOf[HTML.List.Item] // TODO Workaround

    def Elements(contents: HTML.List.Item*) =
      HTML.List.Unordered(contents: _*)
        .css("nav", "navbar-nav")

    def Form(contents: Widget[_]*) =
      HTML.Container.Generic(contents: _*)
        .css("navbar-form")

    def Right(contents: Widget[_]*) =
    HTML.Container.Generic(contents: _*)
      .css("navbar-right")

    def Navigation(contents: Widget[_]*) =
      HTML.Container.Generic(contents: _*)
        .css("nav", "navbar-nav")
  }

  // TODO Find a better solution.
  case class Checkbox(contents: Widget[_]*) extends Widget[Checkbox] {
    val checkbox = HTML.Input.Checkbox()

    val rendered =
      HTML.Container.Generic(
        ControlLabel(
          checkbox,
          HTML.Container.Inline(contents: _*)
        )
      ).css("checkbox")
       .rendered

    def bind(data: Channel[Boolean], flush: Channel[Unit] = Channel()) = {
      checkbox.bind(data, flush)
      this
    }
  }

  trait AlertType { val cssTag: String }
  object AlertType {
    case object Success extends AlertType { val cssTag = "alert-success" }
    case object Info extends AlertType { val cssTag = "alert-info" }
    case object Warning extends AlertType { val cssTag = "alert-warning" }
    case object Danger extends AlertType { val cssTag = "alert-danger" }
  }

  def Alert(alertType: AlertType)(contents: Widget[_]*) =
    HTML.Container.Generic(contents: _*)
      .css(s"alert ${alertType.cssTag}")
      .attribute("role", "alert")

  def Panel(contents: Widget[_]*) =
    HTML.Container.Generic(
      HTML.Container.Generic(
        contents: _*
      ) .css("panel-body")
    ) .css("panel panel-default")

  object ListGroup {
    case class Group(contents: Widget.List.Item[_]*) extends Widget.List[Group] {
      val rendered = HTML.Container.Generic(contents: _*)
        .css("list-group")
        .rendered
    }

    case class PageItem[T <: PageItem[T]](contents: Widget[_]*) extends Widget.List.Item[T] {
      val widget = HTML.Anchor(contents: _*)
        .css("list-group-item")
      val rendered = widget.rendered

      def url(value: String) = {
        widget
          .asInstanceOf[HTML.Anchor] // TODO Workaround
          .url(value)
        this
      }

      def active(ch: Channel[Boolean]) = {
        widget.cssCh(ch, "active")
        this
      }
    }

    // clearfix is needed in conjunction with PullRight()
    case class Item[T <: Item[T]](contents: Widget[_]*) extends Widget.List.Item[T] {
      val widget = HTML.Container.Generic(contents: _*)
        .css("list-group-item", "clearfix")
      val rendered = widget.rendered

      def active(ch: Channel[Boolean]) = {
        widget.cssCh(ch, "active")
        this
      }
    }

    def ItemHeading(contents: Widget[_]*) =
      HTML.Heading.Level4(contents: _*)
        .css("list-group-item-heading")

    def ItemText(contents: Widget[_]*) =
      HTML.Paragraph(contents: _*)
        .css("list-group-item-text")
  }

  object Grid {
    trait ColumnType { val cssTag: String }
    object ColumnType {
      case object ExtraSmall extends ColumnType { val cssTag = "col-xs" }
      case object Small extends ColumnType { val cssTag = "col-sm" }
      case object Medium extends ColumnType { val cssTag = "col-md" }
      case object Large extends ColumnType { val cssTag = "col-lg" }
    }

    def Column(columnType: ColumnType, level: Int)(contents: Widget[_]*) =
      HTML.Container.Generic(contents: _*)
        .css(s"${columnType.cssTag}-$level")

    def Row(contents: Widget[_]*) =
      HTML.Container.Generic(contents: _*)
        .css("row")
  }
}
