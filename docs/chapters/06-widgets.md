# Widgets
A widget is a type-safe abstraction for an element displayed by the browser. The entire page layout is described using widgets. Thus, widget instantiations can be nested. Furthermore, custom widgets can be defined for better code reuse. A custom widget is usually composed of other widgets, changing their attributes such as CSS tags.

Instead of accessing DOM elements using ``getElementById()``, a widget doesn't have any ID by default. Instead, it maintains a reference to the DOM element. This way, widgets that may have the same ID cannot collide and no ill-defined type-casts may occur.

Mutation methods on a widget return the instance. This allows to arbitrarily nest widgets and change their attributes by chaining method calls, without the need to store the widget in a local variable.

## HTML
Widok provides widgets for many HTML elements. The bindings have a more intuitive naming than their HTML counterparts, although aliases were defined, too. The module the HTML widgets reside in is ``org.widok.bindings.HTML``. If your project doesn't define any conflicting types, it is safe to import the whole contents into the namespace.

|  **Alias**   |        **Widget**       |               **Notes**                |
|----------|---------------------|------------------------------------|
| section  | Section             |                                    |
| header   | Header              |                                    |
| footer   | Footer              |                                    |
| nav      | Navigation          |                                    |
| h1       | Heading.Level1      |                                    |
| h2       | Heading.Level2      |                                    |
| h3       | Heading.Level3      |                                    |
| h4       | Heading.Level4      |                                    |
| h5       | Heading.Level5      |                                    |
| h6       | Heading.Level6      |                                    |
| p        | Paragraph           |                                    |
| b        | Text.Bold           |                                    |
| strong   | Text.Bold           |                                    |
| i        | Text.Italic         |                                    |
| small    | Text.Small          |                                    |
| br       | LineBreak           |                                    |
| hr       | HorizontalLine      |                                    |
| div      | Container.Generic   |                                    |
| span     | Container.Inline    |                                    |
| raw      | Raw                 | ``span`` with ``innerHTML``        |
| form     | Form                |                                    |
| button   | Button              |                                    |
| label    | Label               |                                    |
| a        | Anchor              |                                    |
| img      | Image               |                                    |
| checkbox | Input.Checkbox      | ``input`` with ``type="checkbox"`` |
| file     | Input.File          | ``input`` with ``type="file"``     |
| select   | Input.Select        | ``input`` with ``type="select"``   |
| text     | Input.Text          | ``input`` with ``type="text"``     |
| password | Input.Password      | ``input`` with ``type="password"`` |
| option   | Input.Select.Option |                                    |
| ul       | List.Unordered      |                                    |
| ol       | List.Ordered        |                                    |
| li       | List.Item           |                                    |
| table    | Table               |                                    |
| thead    | Table.Head          |                                    |
| th       | Table.HeadColumn    |                                    |
| tbody    | Table.Body          |                                    |
| tr       | Table.Row           |                                    |
| td       | Table.Column        |                                    |
| cursor   | Cursor              |                                    |

### Aliases
By importing ``org.widok.html._`` you can use regular HTML tags instead of the more verbose notations.

## Usage
A widget inherits from the type ``Widget``. Widgets are implemented as ``case class``es and can therefore be used like regular function calls. The simplest widget is ``Raw()`` which allows to render HTML markup:

```scala
val widget = Raw("<b><i>Text</i></b>")
```

This is equivalent to:

```scala
val widget = Text.Bold(
  Text.Italic("Text")
)
```

Most widgets take children. If this is the case, child widgets are passed per convention with the constructor. Widget parameters are set using chainable method calls:

```scala
Anchor(
  Text.Bold("Wikipedia")
).url("http://en.wikipedia.org/")
 .title("en.wikipedia.org")
```

## Writing custom widgets
Widgets should be designed with type-safety in mind. For example, the only children ``List.Unordered()`` accepts are instances of ``List.Item``. When creating custom widgets, think of a class hierarchy which closely resembles the intended nesting. This will allow to catch usage errors during compile-time.

A custom widget may be defined as follows:

```scala
case class Panel(contents: View*) extends Widget[Panel] {
  val rendered = DOM.createElement("div", contents)
  css("panel")
  css("panel-default")
}
```

This corresponds to:

```html
<div class="panel panel-default">
	... rendered children ...
</div>
```

If a custom widget doesn't need to be used as a type, it is sufficient to define it as a function:

```scala
def Panel(contents: View*) = Container.Generic(contents: _*)
  .css("panel")
  .css("panel-default")
```

## Binding to events
A widget provides functionality to interact with the DOM. Methods with the prefix ``on*()`` exist for all events and take a callback.

To listen to JavaScript's ``onclick`` and ``ondblclick`` events of a button, write:

```scala
Button("Click")
  .onClick(e => println("Click: " + e.timeStamp))
  .onDoubleClick(e => println("Double click: " + e.timeStamp))
```

All DOM events are published as channels. A channel produces data which is passed on to its subscribers. The above is a shortcut for:

```scala
val btn = Button("Click")
btn.click.attach(...)
btn.doubleClick.attach(...)
```

This allows for an event to have multiple subscribers. This is important in web applications where data gets propagated to various layers of the application. For example, consider a shopping cart where the user updates the quantity of a certain product. At the same time the header needs to get updated with the newly calculated price. Making the DOM events available as streams widens the range of possibilities. As ``click`` is a stream of events, we could decide to take into account only the first event:

```scala
btn.click.head.attach(e => println(e.timeStamp))
```

Another prominent use case of channels are dynamic changes of widgets, such as the visibility:

```scala
HTML.Container.Generic("Button clicked")
  .show(btn.click.head.map(_ => false))
```

``show()`` expects a Boolean channel. Depending on the values that are sent to the channel a widget is shown or not. Here, the widget is hidden as soon as we click the button.

Data propagation mechanisms are explained in more detail in the next chapter ['Reactive programming'](#reactive-programming).

## Composed widgets
Widok provides a couple of composed widgets without external rendering dependencies. They are defined in the package ``org.widok.widgets``:

- ``LoremIpsum``: Prints Lorem Ipsum as a paragraph
- ``Placeholder``: Generates placeholder images on-the-fly

## Implicits
Widok defines a couple of implicits to make your code more concise. For example, if there is only one element you may drop the ``Inline()`` and write:

```scala
def view() = HTML.Paragraph("Litwo! Ojczyzno moja!")
```

Instead of:


```scala
def view() = Inline(HTML.Paragraph("Litwo! Ojczyzno moja!"))
```

Another implicit is evaluated here, which converts the string into a widget. There are also implicits to render buffers and channels.

