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

    def apply(styles: Style*)(contents: View*) =
      HTML.Container.Generic(contents: _*)
        .css(styles.map(_.cssTag): _*)
  }

  // TODO Add missing ones.
  trait Glyphicon { val cssTag: String }
  object Glyphicon {
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
    case object Minus extends Glyphicon { val cssTag = "glyphicon-minus" }
    case object Remove extends Glyphicon { val cssTag = "glyphicon-remove" }
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

  def HorizontalForm(contents: View*) =
    HTML.Form(contents: _*)
      .css("form-horizontal")
      .attribute("role", "form")

  def FormGroup(role: Role = Role.None)(contents: View*) = {
    val res = HTML.Container.Generic(contents: _*)
      .css("form-group")

    if (role == Role.None) res
    else res.attribute("role", role.value)
  }

  def InputGroup(contents: View*) =
    HTML.Container.Generic(contents: _*)
      .css("input-group")

  def ControlLabel(contents: View*) =
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

    def apply(style: ReadChannel[Label.Style])(contents: View*) =
      HTML.Container.Generic(contents: _*)
        .css("label")
        .cssCh(style.map(_.toString))
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

  object Button {
    trait Size { val cssTag: String }
    object Size {
      case object Normal extends Size { val cssTag = "" }
      case object ExtraSmall extends Size { val cssTag = "btn-xs" }
      case object Small extends Size { val cssTag = "btn-sm" }
      case object Medium extends Size { val cssTag = "btn-md" }
      case object Large extends Size { val cssTag = "btn-lg" }
    }

    trait Type { val cssTag: String }
    object Type {
      case object Default extends Type { val cssTag = "btn-default" }
      case object Success extends Type { val cssTag = "btn-success" }
      case object Info extends Type { val cssTag = "btn-info" }
      case object Warning extends Type { val cssTag = "btn-warning" }
      case object Danger extends Type { val cssTag = "btn-danger" }
    }

    def apply(size: Size = Size.Normal,
              `type`: Type = Type.Default)
             (contents: View*) =
      HTML.Button(contents: _*)
        .css("btn", `type`.cssTag, size.cssTag)
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

        Bootstrap.Navigation.Item(currentTab.map(_ == tab))(anchor)
      }

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

    def Item(active: ReadChannel[Boolean] = Channel())(contents: View*): HTML.List.Item =
      HTML.List.Item(contents: _*)
        .cssCh(active, "active")
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

    def Leaf(url: String, active: Channel[Boolean] = Channel())(contents: View*): HTML.List.Item =
      HTML.List.Item(
        HTML.Anchor(contents: _*)
          .url(url)
      ).cssCh(active, "active")

    def Branch(contentsCaption: Widget[_]*)(contents: HTML.List.Item*): HTML.List.Item =
      HTML.List.Item(
        HTML.Anchor(
          HTML.Container.Inline(contentsCaption: _*),
          HTML.Container.Inline().css("caret")
        ).css("dropdown-toggle"),
        HTML.List.Unordered(contents: _*)
          .css("dropdown-menu")
          .attribute("role", "menu")
      ).css("dropdown")

    def Elements(contents: HTML.List.Item*) =
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
    case class Group(contents: Widget.List.Item[_]*) extends Widget.List[Group] {
      val rendered = HTML.Container.Generic(contents: _*)
        .css("list-group")
        .rendered
    }

    case class PageItem(contents: View*) extends Widget.List.Item[PageItem] {
      val widget = HTML.Anchor(contents: _*)
        .css("list-group-item")
      val rendered = widget.rendered

      def url(value: String) = {
        widget.url(value)
        this
      }

      def active(ch: Channel[Boolean]) = {
        widget.cssCh(ch, "active")
        this
      }
    }

    // clearfix is needed in conjunction with PullRight()
    case class Item(contents: View*) extends Widget.List.Item[Item] {
      val widget = HTML.Container.Generic(contents: _*)
        .css("list-group-item", "clearfix")
      val rendered = widget.rendered

      def active(ch: Channel[Boolean]) = {
        widget.cssCh(ch, "active")
        this
      }
    }

    def ItemHeading(contents: View*) =
      HTML.Heading.Level4(contents: _*)
        .css("list-group-item-heading")

    def ItemText(contents: View*) =
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

    def Column(columnType: ColumnType, level: Int)(contents: View*) =
      HTML.Container.Generic(contents: _*)
        .css(s"${columnType.cssTag}-$level")

    def Row(contents: View*) =
      HTML.Container.Generic(contents: _*)
        .css("row")
  }
}
