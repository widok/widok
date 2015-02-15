# Widgets
A widget represents an element to be displayed by the browser. Instances of widgets can be nested while enforcing type-safety. Custom widgets can be defined. The idea here is to compose a widget of smaller, existing ones.

## HTML
Widok provides widgets for many HTML elements. The bindings have a more intuitive naming than their HTML counterparts. The module they reside in is ``org.widok.bindings.HTML``. Although it is possible to import the whole contents, it is advisable to address the widgets using a qualified access. Usually, a project needs to define its own widgets which most likely will shadow widgets from the HTML bindings. Instead, the custom widgets could be imported into the namespace. These custom widgets in turn will depend on the HTML widgets and only extend them with CSS tags for instance.

|   Tag   |       Widget      |          Notes           |
|---------|-------------------|--------------------------|
| h1      | Heading.Level1    |                          |
| h2      | Heading.Level2    |                          |
| h3      | Heading.Level3    |                          |
| h4      | Heading.Level4    |                          |
| h5      | Heading.Level5    |                          |
| h6      | Heading.Level6    |                          |
| p       | Paragraph         |                          |
| b       | Text.Bold         |                          |
| small   | Text.Small        |                          |
| span    | Raw               | sets ``innerHTML``       |
| img     | Image             |                          |
| br      | LineBreak         |                          |
| button  | Button            |                          |
| section | Section           |                          |
| header  | Header            |                          |
| footer  | Footer            |                          |
| nav     | Navigation        |                          |
| a       | Anchor            |                          |
| form    | Form              |                          |
| label   | Label             |                          |
| input   | Input.Text        | sets ``type="text"``     |
| input   | Input.Checkbox    | sets ``type="checkbox"`` |
| select  | Input.Select      |                          |
| ul      | List.Unordered    |                          |
| ol      | List.Ordered      |                          |
| li      | List.Item         |                          |
| div     | Container.Generic |                          |
| span    | Container.Inline  |                          |

### Usage
A widget is always an instance of ``Widget`` and can be used like a function:

```scala
val widget = HTML.Raw("<b><i>Text</i></b>")

// This is equivalent to:
val widget2 = Text.Bold(
  Text.Italic("Text")
)
```

Most widgets either expect parameters or children. However, there are also widgets which expect both:

```scala
Anchor("http://en.wikipedia.org/")(
    Text.Bold("Wikipedia"))
```

> *Hint:* Use the code completion of your IDE to figure out which widgets are available and which parameters to pass.

## Creating custom widgets
Widgets should be designed to be restrictive. For example, the only children ``List.Unordered()`` accepts are instances of ``List.Item``. For custom widgets, create a class hierarchy which closely resembles the intended nesting of the elements. This will turn out to be helpful because you implicitly establish type-safety for CSS components. When widgets are changed, this will catch usage errors during compile-time.

A custom widget may be defined as follows:

```scala
def Panel(contents: Widget*) =
    HTML.Container.Generic(
      HTML.Container.Generic(
        contents: _*
      ).css("panel-body")
    ).css("panel", "panel-default")
```

This corresponds to:

```html
<div class="panel panel-default">
    <div class="panel-body">
        ... contents of the widget's children ...
    </div>
</div>
```

## Binding to events
Each widget provides useful functions to better interact with the DOM. Instead of setting IDs on elements and requesting elements using ``getElementById()``, we are constantly working with objects which is less error-prone.

To bind to the click event to a button, write:

```scala
import org.scalajs.dom.MouseEvent

...

Button(Glyphicon.Book)("Show timestamp")
    .bindMouse(Event.Mouse.Click, (e: MouseEvent) => println(e.timeStamp))
```

Note that methods on a widget return the instance of the widget. This allows to arbitrarily nest widgets and change their attributes, without storing a reference to the widget in a local variable.

The above example does not seem all too different from attaching a callback like in JavaScript. This is also what happens implicitly. In fact, the second argument of ``bindMouse()`` expects a *Channel*, a concept we will introduce in the next chapter. A channel produces data which is passed on to its subscribers. In the above example, a Scala implicit turns the lambda function into a channel. But similarly, we could have written:

```scala
val click = Channel[MouseEvent]()
click.attach(e => println(e.timeStamp))
...

Button(Glyphicon.Book)("Show timestamp")
    .bindMouse(Event.Mouse.Click, click)
```

The advantage may not be obvious on first sight, but a channel can have multiple subscribers. This is important in web applications where data gets propagated to various layers of the application. For example, consider a shopping cart. Items get modified in the product listing. At the same time the header needs to get updated with the newly calculated price.

Now that ``click`` is a stream of events, we could decide to take into account only the first event:

```scala
click.head.attach(e => println(e.timeStamp))
```

Another prominent use case of channels are dynamic changes of widgets, such as the visibility:

```scala
HTML.Container.Generic("Button clicked")
    .show(click.head.map(_ => false))
```

``show()`` expects a boolean channel. Depending on the values that are sent to the channel a widget is shown or not. Here, we hide the widget as soon as we click the button.

The chapter [Data propagation](#data-propagation) deals with channels in detail.

## Composed widgets
Widok provides a couple of composed widgets without external rendering dependencies. They are defined in the package ``org.widok.widgets``:

- ``LoremIpsum``: Prints Lorem Ipsum as a paragraph
- ``Placeholder``: Generates placeholder images on-the-fly

## Implicits
Widok defines a couple of implicits to make your code more concise. For example, if there is only one element you may drop the ``Inline()`` and write:

```scala
def view() = HTML.Paragraph("Litwo! Ojczyzno moja!")
```

Another implicit is evaluated here, which converts the string into a widget. There also implicits for most reactive data structures.

## Aliases
By importing ``import org.widok.html._`` you can use regular HTML tags instead of the more verbose notations.

