# Bindings
This chapter deals with third-party CSS frameworks for which Widok provides typed bindings.

## Bootstrap
Bootstrap is a "framework for developing responsive, mobile first projects on the web." See the [project page](http://getbootstrap.com/) for more information.

To use the bindings, it may be desirable to import its entire namespace:

```scala
import org.widok.bindings.Bootstrap._
```

Bootstrap's components closely resemble their HTML counterparts. For example:

```html
<button type="button" class="btn btn-default">
  <span class="glyphicon glyphicon-align-left"></span>
</button>
```

This translates to:

```scala
Button(Glyphicon.AlignLeft())
```

Bootstrap widgets expect a list of arguments which denotes child widgets. The configuration can be controlled by usual method calls on the widget. If a widget conceptually doesn't have any children, then its arguments are used for the configuration instead.

### External stylesheet
For the bindings to work without sbt-web, add the latest Bootstrap stylesheet to the ``head`` tag of your ``application.html`` file. You can either keep a local copy of the stylesheet or use a CDN:

```html
<link
  rel="stylesheet"
  href="https://maxcdn.bootstrapcdn.com/
bootstrap/3.3.2/css/bootstrap.min.css">
```

Please keep in mind that the pre-built stylesheet comes with certain restrictions, like the font path being hard-coded.

### Label
Every widget is equipped with a method ``label(value: Style)`` that allows attaching a Bootstrap label like ``label-info`` to it:

```scala
span("Text").label(Label.Info)
```

``TextContainer(styles: Style*)`` generates a container with a list of styles.

### Glyphicons
Glyphicons are simple function calls, for example: ``Glyphicon.User()``. All Bootstrap glyphicons are supported, although the naming was changed to camel-case.

### Forms
Forms can be validated on-the-fly. For each field a custom validator may be written. ``validator.errors()`` will render the textual error. Instead of showing the error underneath a field, this call can be placed anywhere, for instance to centralise all errors. ``validate()`` is defined on every widget and sets the ``has-error`` CSS tag if a field is invalid. The initial validation is triggered when the user presses the submit button. ``validator.check()`` will perform the first validation and return ``true`` if all fields are valid. If at least one input field was invalid, the submit button is kept disabled as long as the input stays wrong.

```scala
val username    = Var("")
val displayName = Var("")

def validateNonEmpty(value: String) =
  if (value.trim.isEmpty) Some("Field cannot be empty")
  else None

implicit val validator = Validator(
  Validation(username,    validateNonEmpty)
, Validation(displayName, validateNonEmpty)
)

Container(
  FormGroup(
    InputGroup(
      InputGroup.Addon(Glyphicon.Globe())
    , Input.Text()
        .placeholder("Display name")
        .size(Size.Large)
        .tabIndex(1)
        .bind(displayName)
    )
    , validator.errors(displayName)
  ).validate(displayName)

  , FormGroup(
      InputGroup(
        InputGroup.Addon(Glyphicon.User())
      , Input.Text()
          .placeholder("Username")
          .size(Size.Large)
          .tabIndex(2)
          .bind(username)
      )
      , validator.errors(username)
    ).validate(username)
  )

, Button("Submit").onClick { _ =>
    if (validator.check()) println("Ok")
  }.enabled(validator.maySubmit)
)
```

Other widgets related to forms are:

- ``HorizontalForm()``
- ``FormGroup()``
- ``ControlLabel()``
- ``InputGroup()``
- ``InputGroup.Addon()``
- ``Input.Text()``
- ``Input.Password()``
- ``Input.Select()``
- ``Button()``
- ``Button.Group()``
- ``Button.Toolbar()``
- ``Checkbox()``

### Layout
Layout-related widgets are:

- ``Footer()``
- ``Container()``
- ``PageHeader()``
- ``Lead()``
- ``PullRight()``

### Navigation
Example:

```scala
val tab1 = Navigation.Tab("Tab 1")
val tab2 = Navigation.Tab("Tab 2")
val currentTab = Var(tab1)

Navigation.renderTabs(Seq(tab1, tab2), currentTab)
```

### Navigation bar
Example for the ``NavigationBar`` widget:

```scala
NavigationBar(
  Container(
    NavigationBar.Header(
      NavigationBar.Toggle()
    , NavigationBar.Brand("Brand name")
    )
  , NavigationBar.Collapse(
      NavigationBar.Elements(
        Item(a(Glyphicon.Dashboard(), " Page 1").url(Routes.page1()))
      , Item(a(Glyphicon.Font(), " Page 2").url(Routes.page2()))
      , NavigationBar.Right(
          NavigationBar.Navigation(
            NavigationBar.Form(
              FormGroup(
                InputGroup(Input.Text())
              , Button(Glyphicon.Search())
              ).role(Role.Search)
            )
          )
        )
      )
    )
  )
)
```

As probably more than one page is going to use the same header, you should create a trait for it. For example, you could define ``CustomPage`` with the header. Then, it only requires you to define the page title and body for every page.

### Alert
Example:

```scala
Alert("No occurrences").style(Style.Danger)
```

### Progress bar
Example:

```scala
val percentage = Var(0.1)
ProgressBar("Caption")
  .style(percentage.map(p => if (p < 0.5) Style.Warning else Style.Success))
  .progress(percentage)
```

### Panel
Example:

```scala
Panel(
  Panel.Heading(Panel.Title3("Panel title"))
, Panel.Body("Panel text")
).style(Style.Danger)
```

### Pagination
Example:

```scala
Pagination(
  Pagination.Item(a("«")).disabled(true)
, Pagination.Item(a("1")).active(true)
, Pagination.Item(a("2"))
, Pagination.Item(a("»"))
)
```

### List groups
Example:

```scala
ListGroup(
  ListGroup.Item(a("Item 1")).active(true),
, ListGroup.Item(a("Item 2"))
, ListGroup.Item(a("Item 3"))
)
```

### Grids
Example:

```scala
Grid.Row(
  Grid.Column(
    "Grid contents"
  ).column(Size.ExtraSmall, 6)
   .column(Size.Medium, 3)
)
```

### Button
Example:

```scala
Button(Glyphicon.User())
  .size(Size.ExtraSmall)
  .onClick(_ => println("Clicked"))
  .title("Button title")
```

``AnchorButton`` provides the functionality of ``Button`` and ``HTML.Anchor``:

```scala
Button(Glyphicon.User())
  .size(Size.ExtraSmall)
  .url("http://google.com/")
  .title("Button title")
```

### Modal
It is most convenient to use the ``ModalBuilder`` to create modals.  On the same page you can define several modals. For example:

```scala
val modal: ModalBuilder = ModalBuilder(
  Modal.Header(
    Modal.Close(modal.dismiss)
  , Modal.Title("Modal title")
  )
, Modal.Body("Modal body")
, Modal.Footer(
    Button("Submit").onClick(_ => modal.dismiss())
  )
)

def body() = div(
  Button("Open").onClick(_ => modal.open())
, modal /* Each modal must be added to the body. It is hidden by default. */
)
```

### Media
Example:

```scala
Media(
  Media.Left(Placeholder("cover", Placeholder.Size(150, 80)))
, Media.Body(
    Media.Heading("Heading")
  , "Description"
  )
)
```

### Breadcrumb
```scala
Breadcrumb(
  Item(a("Item 1"))
, Item(a("Item 2")).active(true)
)
```

### Table
To use a Bootstrap table, use ``Table()`` and ``Table.Row()`` which in contrast to ``table()`` and ``tr()`` provide Bootstrap-related styling options:

```scala
Table(
  thead(
    tr(
      th("Date")
    , th("Quantity")
    )
  )

, tbody(
    Table.Row(td("01.01.2015"), td("23")).style(Style.Info)
  , Table.Row(td("02.01.2015"), td("42")).style(Style.Danger)
  )
)
```

### Typeahead
Example:

```scala
val allMatches = Map(0 -> "First", 1 -> "Second", 2 -> "Third")
def matches(input: String): Seq[(Int, String)] =
  allMatches.filter { case (k, v) => v.startsWith(input) }.toSeq
def select(selection: Int) { println(s"Selection: $selection") }

Typeahead(Input.Text(), matches, select)
```

## Font-Awesome
The Font-Awesome bindings include all icons in camel-case notation. For convenience, rename the object when you import it:

```scala
import org.widok.bindings.{FontAwesome => fa}
```

Using the [``user`` icon](http://fortawesome.github.io/Font-Awesome/icon/user/) is as simple as writing:

```scala
fa.User()
```

This translates to:

```html
<span class="fa fa-user"></span>
```

