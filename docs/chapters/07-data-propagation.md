# Data Propagation
Due to its nature, user interfaces (UIs) are notoriously heavily data-driven. Values do not only need to be displayed once, but continuously modified as the user interacts with the interface.

A higher interactivity results in more data dependencies and ultimately in more complex code. Imperative code in particular is prone to this shortcoming as dependencies must be modelled manually. With web application getting increasingly more interactive, solutions with clean and concise code are needed.

Widok advocates a flow-driven approach: Values are expressed in a stream-like data structure which we call *channel*. A channel is equipped with operations that allow to easily express data dependencies. Channels can have children to which the incoming values are propagated.

Built on top of channels, there is another data structure called *aggregates*. Channels model single values while aggregates represent a collection of values.

A key concept of channels and aggregates is that data may be rendered directly to the DOM without any intermediate caching. Whenever a new value is pushed to a channel, an atomic update takes place, only changing the associated DOM node. Similarly, this is respected when inserting, updating or removing items in an aggregate.

For an application that makes heavy use of data propagation, see our [TodoMVC implementation](https://github.com/widok/todomvc).

The proper functioning of each operation is backed by [test cases](https://github.com/widok/widok/tree/master/src/test/scala/org/widok). These serve as a complementary documentation as only a handful of operations are explained in full detail here.

## Channels
A channel is a multiplexer for typed messages (immutable values) that it receives. Values sent to the channel get propagated to the observers that have been attached to the channel, in the same order as they were added.

As outlined above, channels are a memory-efficient model and may be bound to widgets. The widget renders the received values on-the-fly. However, the lack of state restricts the possibilities. It may be desired to obtain the current value of a channel, change it or perform more elaborate operations that inherently require caching. This was taken into consideration and explicit caching can be performed.

Here is a simple example for a channel that receives integers and prints these on the console:

```scala
val ch = Channel[Int]()  // initialise
ch.attach(println)       // attach observer
ch := 42                 // produce value
```

> **Note:** The ``:=`` operator is a shortcut for the method ``produce``.

Channels define useful operations to express data dependencies:

```scala
val ch = Channel[Int]()
ch.filter(_ > 3)
  .map(_ + 1)
  .attach(println)
ch := 42 // 43 printed
ch := 1  // nothing printed
```

These operations are well-known methods such as ``map()``, ``filter()``, ``take()`` etc. As they return instances of ``Channel``, they can be chained as in the example.

A particular operator defined on channels is the addition. By adding up channels a new container channel is constructed. All values produced by the operands are propagated to this resulting channel and vice-versa (two-way propagation), but not amongst the operands:

```scala
val cont = ch + ch2  // more operands are possible, too
ch  := 42            // propagated to cont
ch2 := 23            // propagated to cont
cont := 65           // propagated to ch and ch2
```

### State channels
State channels are channels that have an initial value. In reactive programming, these are known as *cold observers*.

The value that the state channel was instantiated with gets propagated upon attaching to the channel. A common use case are user interfaces. Usually, channels are set up before the widgets. The channels are then bound to the widgets. This renders the initial value directly in the DOM. Otherwise, it would be necessary to send the initial value to the channel manually as soon as the widget was rendered.

The following example visualises the difference in behaviour:

```scala
val ch = Channel.unit(42)
ch.attach(println) // prints 42

val ch2 = Channel[Int]()
ch2 := 42 // lost as ch2 does not have any observers
ch2.attach(println)
```

> **Note:** ``Channel.unit(value)`` is equal to ``StateChannel(value)``.

The argument is evaluated lazily and can also point to a mutable variable:

```scala
var counter = 0
val ch = Channel.unit(counter)
ch.attach(value => {counter += 1; println(value)}) // prints 0
ch.attach(value => {counter += 1; println(value)}) // prints 1
```

The following example illustrates a conceptual issue that arises with the use of chaining:

```scala
val ch = Channel.unit(42)
val ch2 = ch.map(_ + 1)
ch2.attach(...) // produces 43?
ch2.attach(...) // produces 43?
```

The question is whether initially produced values get propagated along the chain of operations.

### Child channels
In order to make all operations work properly on state channels, the notion of *child channels* was introduced. The previously given example is therefore well-defined. A child channel delays the propagation of the initial value until an observer is attached.

In order to figure out whether an operation supports this behaviour, it is sufficient to investigate its return type. The child channel behaviour only makes sense on operations that do not need more than one produced value to propagate it. A counterexample is ``skip()`` which conceptually must skip at least one element; the initial value will therefore never be propagated:

```scala
val ch = Channel.unit(42)
ch.skip(1) // returns Channel
  .attach(...)
```

> **Note:** Higher-order functions must not have any side-effects when used in operations on state channels.
>
> ```scala
> val ch = Channel.unit(42)
> ch.map(println).attach(_ => ())
> ```
>
> The example does not behave as expected and prints 42 two times instead of only once. This design decision was made consciously as to keep the implementation of operations more simple.

### Cached channels
For better performance, channels do not cache the produced values. Some operations cannot be implemented without access to the current value, though. Therefore, *cached channels* were introduced.

For example, ``update()`` is an operation that would not work without caching. It takes a function which modifies the current value:

```scala
val ch = CachedChannel[Test]()
ch.attach(println)
ch := 2
ch.update(_ + 1) // produces 3
```

``value()`` is another helpful operation. It creates a channel lens. If a channel is made up of a ``case class``, you could obviously use ``.map(_.field)`` to obtain a channel for a field. However, as with lenses in functional programming, a back channel is desired which composes a new value with this field changed. This also works with nested values as in the following example:

```scala
case class Base(a: Int)
case class Test(a: String, b: Base)

val ch = CachedChannel[Test]()
val lens = ch.value[Int](_ >> 'b >> 'a)

ch.attach(println)

ch := Test("hello world", Base(1))
lens := 2 // produces Test("hello world", Base(2)) on ch
```

In order to get a cached version of an existing channel, use the method ``cache``:

```scala
val cache = ch.cache
ch := 1
cache.update(_ + 1) // produces 2 on ``ch``
```

Use the method ``unique`` to produce a value if it is the first or different from the previous one. A use case is sending HTTP requests upon user input:

```scala
val ch = cache.unique
ch.attach { query =>
    // perform HTTP request
}
```

> **Note:** As a ``CachedChannel`` inherits from ``Channel``, all channel operations are available as well.

### Widgets
A channel can be connected with one or more widgets. Most widgets provide two-way binding. To bind a channel to a widget, use the method ``bind()`` or its specialisations ``bindRaw()``, ``bindWidget()`` etc.

When associating a channel to multiple widgets, the contents amongst them is synchronised automatically:

```scala
val ch = Channel.unit("Hello world")
def contents() = Seq(
  Input.Text().bind(ch, live = true),
  Input.Text().bind(ch)
)
```

This creates two text fields. When the page is loaded, both have the same content: "Hello world". If the user changes the text of the first field, the second text field is updated on-the-fly. The second field has the live mode disabled and an enter press is required before the change gets propagated to the first text box.

> **Note:** ``bind()`` may only be used once. If you want to have multiple event handlers, you can chain these using the ``+`` operator that was mentioned above.

Channels are consistently used for all kind of DOM events. This allows to connect even two unrelated widgets. The following example connects a text field with a button:

```scala
val query = Channel[String]()
val click = Channel[Unit]()

query.attach(println)

def contents() = Seq(
  Input.Text().bind(query, click),
  Button().bind(click)
)
```

The second parameter of ``bind()`` on the text field is a channel that "flushes" its contents. Sending a message to it is equivalent to pressing enter in the field. The button is bound to the same channel. Pressing the button results in the text field to produce its contents to ``query``. If widgets were using callbacks instead, the above would be a lot harder to implement.

The method ``cssCh()`` on widgets sets a CSS flag only if the expected boolean channel produces ``true`` values, otherwise it is unset:
```scala
widget
 .cssCh(editing, "editing")
```

An implicit is defined so that string channels can be used easily:

```scala
val name = Channel[String]()
def contents() = Heading.Level1("Hello ", name)
```

``name`` is converted into a widget and updates the DOM when new values are produced.

Another implicit is provided for ``Channel[Widget]``. You can use ``map()`` on a channel to create a widget stream. These are rendered automatically. If the widget type is always the same and it provides a ``bind()`` method, this is to be preferred instead. ``bind()`` does not recreate the widget itself and is therefore more efficient.

## Aggregates
Aggregates allow to efficiently deal with lists of changing values. An aggregate is implemented as a list of channels.

You may be wondering what is the purpose of having ``Aggregate[T]`` while ``Channel[Seq[T]]`` could be used. If you need to operate on a subsets, it is hard to do so with the latter approach. What's more, it is also a costly operation. If, however, the elements of your list are constant, you should use the channel approach. Widgets provide ``bind()`` for both variants.

```scala
val agg = Aggregate[Int]()
val ch = agg.append() // adds a new row; returns Channel
ch := 42              // sets row value to 42
```

As with channels, an added row is lost if no observer was added beforehand. In practice, attaching a self-written observers is not as common as for channels. Widok already provides important operations and aggregates are most useful in connection with widgets. The general recommendation is to never use ``attach()`` to keep the code clean. Observers can get messy, especially if back-propagation is involved. If an observer becomes necessary, in almost every case this indicates that an operation should be implemented instead. Refer to the test cases for more information on writing observers for aggregates.

To observe the size of an aggregate, use ``size`` which returns a ``Channel[Int]``:
```scala
agg.size.attach(println)
```

As aggregates are just containers for channels, it is possible to obtain sub-lists and propagate changes back:

```scala
val agg = Aggregate[Int]()
val filter = agg.filter(_ % 2 == 0)

agg.append(3)  // not propagated
agg.append(4)  // propagated
agg.append(5)  // not propagated

filter.clear() // back-propagates the deletion to agg
               // agg will only contain 3 and 5.
```

> **Note:** Not every operation implements back-propagation.

### Cached aggregates
Similarly to channels, there is a cached counterpart which stores the most recent value for each row.

```scala
val agg = Aggregate[Int]()
val cached = agg.cache
```

The method ``filterCh()`` expects a channel that is producing filter functions:

```scala
val f = Channel[Int => Boolean]()
val filtered = cached.filterCh(f)

f := _ > 1
f := _ > 2
```

Every time a new filter function is produced to ``f``, the filter is applied on all elements from ``agg`` and ``filtered`` subsequently only contains those matching ones.

### Widgets
All list-like widgets (such as tables) provide the method ``bind()``:

```scala
List.Unordered().bind(agg) { ch =>
  List.Item(ch)
}
```

Aggregates implement many operations which interact nicely with widgets: ``isEmpty`` could be used to hide widgets if an aggregate is empty:

```scala
val agg = Aggregate[Int]()

val widget = HTML.Container.Inline("The list is empty.")
  .show(agg.isEmpty)

val widget2 = HTML.Container.Inline("The list is not empty.")
  .show(agg.nonEmpty)
```

## Debugging
Although, channels and aggregates are used by widgets, they do not depend on JavaScript features. You can therefore debug their use directly on the console:

```bash
$ sbt console
import org.widok._
val ch = Channel[Int]()
ch.attach(println)
ch := 42
```

## API documentation
- [Channel](http://widok.github.io/api/latest/index.html#org.widok.Channel)
- [Aggregate](http://widok.github.io/api/latest/index.html#org.widok.Aggregate)

