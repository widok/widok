# Reactive programming
## Motivation
User interfaces are heavily data-driven. Values do not only need to be displayed once, but continuously modified as the user interacts with the interface. Interactivity requires data dependencies which ultimately lead to deeply intertwined code. Imperative code in particular is prone to this shortcoming since dependencies are hard to express. As web applications are increasingly more interactive, a flow-driven approach is desirable. Focussing on flows, the essence of the program will be to specify the data dependencies and how values propagate to the user interface and back.

To tackle this issue, Widok follows a reactive approach. Consider an application to visualise stock market data. You are listening to a real-time stream producing values. Then, you want to display only the most current stock price to the user. This is solved by creating a 'container' which is bound to a DOM node. Whenever you feed a new stock price to it, an atomic update takes place in the browser, only changing the value of the associated DOM node.

Another example is a monitoring service which allows you to control on-the-fly the log level of a web application. A select box will list all possible log levels, like ``debug`` or ``critical``. When the page is first loaded, it obtains the current log level from the server. Changing its value, however, must back-propagate and send the selection to the server. All other clients that are connected are notified of the change as well.

For a simple application that illustrates client-side data propagation, see our [TodoMVC implementation](https://github.com/widok/todomvc).

## Reactive data structures
Widok uses [MetaRx](https://github.com/MetaStack-pl/MetaRx), which currently implements four reactive data structures:

- **Channels:** single values like ``T``
- **Buffers:** lists like ``Seq[T]``
- **Dictionaries:**  maps like ``Map[A, B]``
- **Sets:**  reactive ``Set[T]``

Make sure to read its documentation before continuing.

## Buffers
Buffers are reactive lists. State changes such as row additions, updates or removals are encoded as delta objects. This allows to reflect these changes directly in the DOM, without having to re-render the entire list. ``Buffer[T]`` is therefore more efficient than ``Channel[Seq[T]]`` when dealing with list changes.

> **Note:** ``Buffer`` will identify rows by their value if the row type is a ``case class``. In this case, operations like ``insertAfter()`` or ``remove()`` will always refer to the first occurrence. This is often not desired. An alternative would be to define a ``class`` instead or to wrap the values in a ``Ref[_]`` object:

```scala
val todos = Buffer[Ref[Todo]]()
ul(
  todos.map { case tr @ Ref(t) =>
    li(
      // Access field `completed`
      checkbox().bind(t.completed)

      // remove() requires reference
    , button().onClick(_ => todos.remove(tr))
    )
  }
)
```

## Binding to Widgets
Reactive data structures interact with user interfaces. These data structures are usually set up before the widgets, so that they can be referenced during the widget initialisation. The most common use case is binding channels to DOM nodes:

```scala
val name = Channel[String]()
def view() = h1("Hello ", name)
```

This example shows one-way binding, i.e. uni-directional communication. ``name`` is converted into a widget, which observes the values produced on ``name`` and updates the DOM node with every change. This is realised by an implicit and translates to ``span().subscribe(name)``.

Another implicit is provided for widget channels, so you can use ``map()`` on any channel to create a widget stream. The widgets are rendered automatically. If the widget type stays the same and it provides a ``subscribe()`` method, use it instead.

On form fields you will need to call ``subscribe()`` by yourself:

```scala
val name = Channel[String]()
def view() = text().subscribe(name)
```

Two-way binding is achieved by using the method ``bind()`` instead of ``subscribe()``. The only difference is that changes are back-propagated. This lets you define multiple widgets which listen to the same channel and synchronise their values:

```scala
val ch = Var("Hello world")
def view() = Inline(
  Input.Text().bind(ch)
, Input.Text().bindEnter(ch)
)
```

This creates two text fields. When the page is loaded, both have the same content: "Hello world". When the user changes the content of the first field, the second text field is updated on-the-fly. The second field requires an enter press before the change gets propagated to the first text field.

Each widget has methods to control its attributes either with static values or channels. For example, to set the CSS tag of a widget use ``widget.css("tag1", "tag2")``. This method is overloaded and you could also pass a ``ReadChannel[Seq[String]]``.

Passing channels is useful specifically for toggling CSS tags with ``cssState()``. It sets CSS tags only when the expected channel produces ``true``, otherwise it unsets the tags:

```scala
widget.cssState(editing, "editing", "change")
```

Other useful functions are ``show()`` and ``visible()``. The former sets the CSS property ``display`` to ``none``, while the latter sets ``visibility`` to ``hidden`` to hide a widget.

As reactive data structures provide streaming operations that return channels, these can be used in widgets. Consider the method ``isEmpty`` that is defined on buffers. You could show a ``span`` depending on whether the list is empty or not:

```scala
val buf = Buffer[Int]()

def view() = Inline(
  span("The list is empty.")
    .show(buf.isEmpty)
, span("The list is not empty.")
    .show(buf.nonEmpty)
, button().onClick(_ => buf += 42)
, button().onClick(_ => buf.clear())
)
```

## Example: Wizard
A wizard with multiple steps can be easily implemented with ``Var``:

```scala
val step = Var(1)

def step1(): Widget[_] = button("Next").onClick(_ => step.update(_ + 1))
def step2(): Widget[_] = ???
def step3(): Widget[_] = ???

def body() = Inline(
  step1().show(step.is(1))
, step2().show(step.is(2))
, step3().show(step.is(3))
)
```

At any time the widgets of every step are in the DOM. If this is not desired, you can load them lazily:

```scala
step.is(1).map {
  case true => step1()
  case false => span()
}
```

