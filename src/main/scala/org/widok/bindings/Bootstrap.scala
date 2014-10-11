package org.widok.bindings

import org.widok._
import org.widok.{Widget => VanillaWidget, InstantiatedRoute, Channel}
import org.widok.bindings.HTML.Input.Checkbox

/**
 * Native widgets for Bootstrap 3 components
 */
object Bootstrap {

  trait TextContainerWidget extends VanillaWidget {
    def withSmall(value: Boolean = true) = {
      setCSS("small", value)
      this
    }

    def withMark(value: Boolean = true) = {
      setCSS("mark", value)
      this
    }

    def withTextLeft(value: Boolean = true) = {
      setCSS("text-left", value)
      this
    }

    def withTextRight(value: Boolean = true) = {
      setCSS("text-right", value)
      this
    }

    def withTextCenter(value: Boolean = true) = {
      setCSS("text-center", value)
      this
    }

    def withTextJustify(value: Boolean = true) = {
      setCSS("text-justify", value)
      this
    }

    def withTextNoWrap(value: Boolean = true) = {
      setCSS("text-nowrap", value)
      this
    }

    def withTextLowerCase(value: Boolean = true) = {
      setCSS("text-lowercase", value)
      this
    }

    def withTextUpperCase(value: Boolean = true) = {
      setCSS("text-uppercase", value)
      this
    }

    def withTextCapitalize(value: Boolean = true) = {
      setCSS("text-capitalize", value)
      this
    }

    def withTextMuted(value: Boolean = true) = {
      setCSS("text-muted", value)
      this
    }

    def withTextPrimary(value: Boolean = true) = {
      setCSS("text-primary", value)
      this
    }

    def withTextSuccess(value: Boolean = true, hover: Boolean = false) = {
      setCSS("text-success", value)
      this
    }

    def withTextInfo(value: Boolean = true, hover: Boolean = false) = {
      setCSS("text-info", value)
      this
    }

    def withTextWarning(value: Boolean = true, hover: Boolean = false) = {
      setCSS("text-warning", value)
      this
    }

    def withTextDanger(value: Boolean = true, hover: Boolean = false) = {
      setCSS("text-danger", value)
      this
    }
  }

  def apply(contents: VanillaWidget*) = {

    contents.map(vanillaWidget => new TextContainerWidget {
      override val rendered = vanillaWidget.rendered
    })
  }

  case class TextContainer(contents: VanillaWidget*) extends Widget {
    val rendered = tag("div", contents: _*)
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
      .withCSS("glyphicon", glyphicon.cssTag)
      .withAttribute("title", caption)

  def HorizontalForm(contents: VanillaWidget*) =
    HTML.Form(contents: _*)
      .withCSS("form-horizontal")
      .withAttribute("role", "form")

  def FormGroup(role: Role = Role.None)(contents: VanillaWidget*) = {
    val res = HTML.Container.Generic(contents: _*)
      .withCSS("form-group")

    if (role == Role.None) res
    else res.withAttribute("role", role.value)
  }

  def InputGroup(contents: VanillaWidget*) =
    HTML.Container.Generic(contents: _*)
      .withCSS("input-group")

  def ControlLabel(contents: VanillaWidget*) =
    HTML.Label()(contents: _*)
      .withCSS("control-label")

  // TODO Improve design.
  def Fix(contents: VanillaWidget*) =
    HTML.Container.Generic(
      HTML.Container.Inline("Fix")
        .withCSS("label label-warning"),
      HTML.Container.Inline(contents: _*)
    ).withCSS("alert alert-danger")

  object Input {
    def Text(placeholder: String = "", autofocus: Boolean = false, autocomplete: Boolean = true) = {
      val text = HTML.Input.Text(placeholder, autofocus, autocomplete)
      text.withCSS("form-control")
      text
    }

    def Select(options: Seq[String], selected: Int = -1) =
      HTML.Input.Select(options, selected)
        .withCSS("form-control")
  }

  object Button {
    trait Size { val cssTag: String }
    object Size {
      case object Normal extends Size { val cssTag = "" }
      case object ExtraSmall extends Size { val cssTag = "btn-xs" }
    }

    // For better usability all buttons should have icons. Therefore
    // None is not the default value.
    def apply(icon: Glyphicon, size: Size = Size.Normal)(contents: VanillaWidget*) = {
      val btn =
        if (icon == Glyphicon.None)
          HTML.Button(contents: _*)
        else
          HTML.Button(Glyphicon(icon) :: contents.toList: _*)

      btn.withCSS("btn btn-default")
    }
  }

  def Footer(contents: VanillaWidget*) =
    HTML.Container.Generic(contents: _*)
      .withCSS("footer")

  def Container(contents: VanillaWidget*) =
    HTML.Container.Generic(contents: _*)
      .withCSS("container")

  def PageHeader(contents: VanillaWidget*) =
    HTML.Container.Generic(contents: _*)
      .withCSS("page-header")

  def Lead(contents: VanillaWidget*) =
    HTML.Container.Generic(contents: _*)
      .withCSS("lead")

  def PullRight(contents: VanillaWidget*) =
    HTML.Container.Inline(contents: _*)
      .withCSS("pull-right")

  def MutedText(contents: VanillaWidget*) =
    HTML.Paragraph(contents: _*)
      .withCSS("text-muted")

  object Navigation {
    case class Tab(name: String)

    def renderTabs(tabs: Seq[Tab], currentTab: Channel[Tab]) = {
      val renderedTabs = tabs.map(tab =>
        Bootstrap.Navigation.Item(currentTab.map(_ == tab))(
          HTML.Anchor()(tab.name).onClick(() => currentTab := tab)))
      Bootstrap.Navigation.Tabs(renderedTabs: _*)
    }

    def Tabs(contents: VanillaWidget*) =
      HTML.List.Unordered(contents: _*)
        .withCSS("nav nav-tabs")
        .withAttribute("role", "tablist")

    def Pills(contents: VanillaWidget*) =
      HTML.List.Unordered(contents: _*)
        .withCSS("nav nav-pills")
        .withAttribute("role", "tablist")

    def Item(active: Channel[Boolean])(contents: VanillaWidget*) = {
      val elem = HTML.List.Item(contents: _*)
      active.attach(value =>
        elem.rendered.className = if (value) "active" else "")
      elem
    }
  }

  object NavigationBar {
    def apply(fixed: NavigationBar.Fixed = FixedTop)(contents: Widget*) =
      HTML.Navigation(contents: _*)
        .withCSS(s"navbar navbar-default ${fixed.cssTag}")
        .withAttribute("role", "navigation")

    trait Fixed {
      val cssTag: String
    }
    case object FixedTop extends Fixed {
      val cssTag = "navbar-fixed-top"
    }
    case object FixedBottom extends Fixed {
      val cssTag = "navbar-fixed-bottom"
    }

    def Toggle() =
       HTML.Button(
         HTML.Container.Inline().withCSS("icon-bar"),
         HTML.Container.Inline().withCSS("icon-bar"),
         HTML.Container.Inline().withCSS("icon-bar")
       ).withCSS("navbar-toggle collapsed")
        .withAttribute("type", "button")

    def Header(contents: VanillaWidget*) =
      HTML.Container.Generic(contents: _*)
        .withCSS("navbar-header")

    def Brand(contents: VanillaWidget*) =
      HTML.Anchor()(contents: _*)
        .withCSS("navbar-brand")

    def Collapse(contents: VanillaWidget*) =
      HTML.Container.Generic(contents: _*)
        .withCSS("collapse navbar-collapse")

    // TODO Add channel parameter to toggle if not active.
    def Leaf(route: InstantiatedRoute)(contents: VanillaWidget*) =
      HTML.List.Item(
        VanillaWidget.Page(route)(contents: _*).withCSS("active"))

    def Branch(contentsCaption: VanillaWidget*)(contents: VanillaWidget*) =
      HTML.List.Item(
        HTML.Anchor()(
          HTML.Container.Inline(contentsCaption: _*),
          HTML.Container.Inline().withCSS("caret")
        ).withCSS("dropdown-toggle"),
        HTML.List.Unordered(contents: _*)
          .withCSS("dropdown-menu")
          .withAttribute("role", "menu")
      ).withCSS("dropdown")

    def Elements(contents: VanillaWidget*) =
      HTML.List.Unordered(contents: _*)
        .withCSS("nav navbar-nav")

    def Form(contents: VanillaWidget*) =
      HTML.Container.Generic(contents: _*)
        .withCSS("navbar-form")

    def Right(contents: VanillaWidget*) =
    HTML.Container.Generic(contents: _*)
      .withCSS("navbar-right")

    def Navigation(contents: VanillaWidget*) =
      HTML.Container.Generic(contents: _*)
        .withCSS("nav navbar-nav")
  }

  def Checkbox(contents: VanillaWidget*) = new VanillaWidget {
    val checkbox = HTML.Input.Checkbox()

    val rendered =
      HTML.Container.Generic(
        ControlLabel(
          checkbox,
          HTML.Container.Inline(contents: _*)
        )
      ).withCSS("checkbox")
       .rendered

    def bind(value: Channel[Boolean]) =
      checkbox.bind(value)
  }

  trait AlertType { val cssTag: String }
  object AlertType {
    case object Success extends AlertType { val cssTag = "alert-success" }
    case object Danger extends AlertType { val cssTag = "alert-danger" }
  }

  def Alert(alertType: AlertType)(contents: Widget*) =
    HTML.Container.Generic(contents: _*)
      .withCSS(s"alert ${alertType.cssTag}")
      .withAttribute("role", "alert")

  def Panel(contents: VanillaWidget*) =
    HTML.Container.Generic(
      HTML.Container.Generic(
      contents: _*
      ) .withCSS(s"panel-body")
    ) .withCSS(s"panel panel-default")

  object ListGroup {
    def Group(contents: VanillaWidget*) =
      HTML.Container.Generic(contents: _*)
        .withCSS("list-group")

    def PageItem(route: InstantiatedRoute, active: Boolean = false)(contents: VanillaWidget*) =
      VanillaWidget.Page(route)(contents: _*)
        .withCSS("list-group-item", if (active) " active" else "")

    // clearfix is needed in conjunction with PullRight()
    def Item(active: Boolean = false)(contents: VanillaWidget*) =
      HTML.Container.Generic(contents: _*)
        .withCSS("list-group-item", "clearfix", if (active) "active" else "")

    def ItemHeading(contents: VanillaWidget*) =
      HTML.Heading.Level4(contents: _*)
        .withCSS("list-group-item-heading")

    def ItemText(contents: VanillaWidget*) =
      HTML.Paragraph(contents: _*)
        .withCSS("list-group-item-text")
  }

  object Grid {
    trait ColumnType { val cssTag: String }
    object ColumnType {
      case object ExtraSmall extends ColumnType { val cssTag = "col-xs" }
      case object Small extends ColumnType { val cssTag = "col-sm" }
      case object Medium extends ColumnType { val cssTag = "col-md" }
      case object Large extends ColumnType { val cssTag = "col-lg" }
    }

    def Column(columnType: ColumnType, level: Int)(contents: VanillaWidget*) =
      HTML.Container.Generic(contents: _*)
        .withCSS(s"${columnType.cssTag}-$level")

    def Row(contents: VanillaWidget*) =
      HTML.Container.Generic(contents: _*)
        .withCSS("row")
  }

}
