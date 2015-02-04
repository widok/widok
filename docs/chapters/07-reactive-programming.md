# Reactive programming
Reactive programming is a paradigm that focuses on:

a) propagation of data, specifically changes, and
b) data flow.

Concretely, a data structure is said to be *reactive* (or *streaming*) if it models its state as streams. It does this by defining change objects (*deltas*) and mapping its operations onto these. The published stream is read-only and can be subscribed. If the stream does not have any subscribers, the state would not get persisted and is lost.

> **Example:** A reactive list implementation could map all its operations like ``clear()`` or ``insertAfter()`` on the two delta types ``Insert`` and ``Delete``. A subscriber can interpret the deltas and persist the computed list in an in-memory buffer.

Another property of a reactive data structure is that it does not only stream deltas, but also state observations. Sticking to the reactive list example, the deltas could allow streaming observations on the list's inherent properties — one being the length, another the existence of a certain element (``contains(value)``).

Finally, a *mutable* reactive data structure is an extension with the sole difference that it maintains an internal state which always represents the computed result after a delta was received. This is a hybrid solution bridging mutable object-oriented objects with reactive data structures. The mutable variant of our reactive list could send its current state when a subscriber is registering. This ultimately leads to better legibility of code as subscribers can register at any point without caring whether the expected data has been propagated already. The second reason is that otherwise we would need multiple instances of mutual objects that interpret the deltas. This is often undesired as having multiple such instances incurs a memory overhead.

To recap, a reactive data structure has four layers:

- **State:** interpretation of the delta stream and "converting" it into a mutable object
- **Mutation operations:** functions to produce deltas on the stream^[These functions do not access the state in any way.]
- **Polling operations:** blocking functions to query the state
- **Streaming operations:** publish the state changes as a stream

Obviously, the first three layers are the very foundation of object-orientation. It is different in that a) modifications are encoded as deltas and b) there are streaming operations.

We focussed on the first component of reactive programming: data propagation. The second component, data flow, is equally important, though. The term "stream" was used several times. This term is polysemous and requires further explanation. In reactive programming there are different types of streams with severe semantic differences.

> [Rx](https://rx.codeplex.com/) (Reactive Extensions) is a contract designed by Microsoft which calls these streams *observables* and defines rules how to properly interact with these. An observable can be subscribed to with an *observer* which has one function for the next element and two auxiliary ones for handling errors and the completion of the stream.
> Furthermore, observables are subdivided into *cold* and *hot* observables^[Source: [leecampbell.blogspot.de](http://leecampbell.blogspot.de/2010/08/rx-part-7-hot-and-cold-observables.html) (4th February 2015)]:
>
> - **Cold observable:** Streams that are passive and start publishing on request
> - **Hot observable:** Streams that are active and publish regardless of subscriptions
>
> There are extensions to Rx which introduce back-pressure^[For instance, [Monifu](https://github.com/monifu/monifu) implements this feature.] to deal with streams that are producing values too fast. This may not be confused with back-propagation which describes those streams where the subscribers could propagate values back to the producer.

This illustrates the diversity of streams. Due to the nature of Widok, streams had to be implemented differently from the outlined ones. Some of the requirements were:

- lightweight design
- support for n-way binding
- usable as the basis for reactive data structures
- provide functionality for resource management
- require little boilerplate to define new operations

To better differentiate from the established reactive frameworks, a less biased term than *observable* was called for and the reactive streams are therefore called *channels* in Widok. The requirements have been implemented as follows: A subscriber is just a function pointer (wrapped around a small object). A channel can have an unlimited number of children whereas each of the child channels know their parent. A function for flushing the content of a channel upon subscription can be freely defined during instantiation^[This function is called by ``attach()`` and produces multiple values which is necessary for some reactive data structures like lists.]. When a channel is destroyed, so are its children. Error handling is not part of the implementation. Similarly, no back-pressure is performed, but back-propagation is implemented for some selected operations like ``biMap()``.

For client-side web development only a small percentage of the interaction with streams require the features observables provide and this does not justify a more complex overall design. It is possible to use a full-fledged model like Rx or Monifu for just those areas of the application where necessary by redirecting (piping) the channel output.

The second cornerstone of reactive programming is data flow. Streams describe data flow in terms of dependencies. Considering you want to plot a line as a graph using the formula $y = mx+b$ and the user provides the values for $m$ and $b$, then you would wrap these inputs around channels and express the dependencies using combinators^[The types in the code only serve illustration purposes]:

```scala
val m = Opt[Int]()
val b = Opt[Int]()

// Produces when user provided `m` and `b`
val mAndB: ReadChannel[(Int, Int)] = m.combine(b)

// Function channel to calculate `y` for current input
val y: ReadChannel[Int => Int] =
  mAndB.map { case (m, b) =>
    (x: Int) => m * x + b
  }
```

The user could listen to ``y`` and whenever it receives a new function, it can just call it for all the ``x`` in the interval of the shown graph. The example shows that messages in streams are not bound to data objects as even immutable functions could be passed around.

The data propagation is illustrated by the following graphic:

![Change propagation for $y=mx+b$](images/data-flow.pdf)

As soon as the user inserts a value for ``m`` as well as ``b``, ``mAndB`` will produce a tuple. Then ``y`` computes the final function.

How channels work in detail is explained in the following sections. This example should only give an intuition of the fundamental concepts and how data dependencies are expressed.

## Implementation
This section explains how reactive data structures are implemented in Widok. The design decisions will be beneficial for you to better understand the API and to design your own reactive data structures.

To leverage the capabilities of Scala's type system, we decided to separate the logic into distinct traits. Each data structure defines six traits which, when combined using the Cake pattern, yield a mutable reactive object without any additional code needed:

```tree
[""
    ["Read" ["Delta"] ["Poll"]]
    ["Write"]
    ["State" ["Disposable"]]
]
```

For a hypothetical reactive data structure ``X`` you would define:

```scala
object X {
    /* Define delta change type */
}

/* Read/write access to state */
trait StateX[T] extends Disposable {
    /* This could be any kind of mutable storage */
    val state: Storage[T] = ...
    /* Channel needed by the other traits */
    val changes: Channel[X.Delta[T]] = ...
    /* Listen to `changes` and persist these in `state` */
    changes.attach { ... }
    /* Free resources */
    def dispose() { changes.dispose() }
}

/* The name may suggest otherwise, but it does not have any access
 * to the state; it only produces delta objects
 */
trait WriteX[T] {
    val changes: WriteChannel[X.Delta[T]]
    /* Also define operations to generate delta change objects */
}

trait DeltaX[T] {
    val changes: ReadChannel[X.Delta[T]]
    /* Also define streaming operations that listen to changes
     * and process these
     */
}

trait PollX[T] {
    val changes: ReadChannel[X.Delta[T]]
    /* Only read-only access is permitted here */
    val state: Storage[T]
    /* Also define streaming operations that need the state */
}

trait ReadX[T] extends DeltaX[T] with PollX[T]

case class X[T]()
    extends ReadX[T]
    with WriteX[T]
    with StateX[T]
```

A call to ``X()`` now yields a mutable reactive instance of our newly defined data structure.

It would have been possible to implement ``X`` as a single class, but the chosen approach offers more flexibility. Each of the traits are exchangeable. There are more possibilities for object instantiations. For example, often a change stream is already available. In this case, ``DeltaX[T]`` could be instantiated with a custom value for ``changes``. The caller can decide whether it needs any of the operations that ``PollX`` defines. Depending on this decision it will either buffer the data or not. This ultimately leads to a more memory-efficient design as the responsibility of memory allocation is often shifted to the caller. It is in some way similar to what Python allows with its ``yield`` expression.

The delta trait has a read-only version of the change stream. It may define operations that apply transformations directly on the stream without building any complex intermediate results. A prominent example would be the higher-order function ``map()``. As ``map()`` works on a per-element basis and does not need any access to the state, it can be implemented efficiently. As a consequence, this allows for chaining: ``list.map(f).map(g).buffer`` would compute the final list at the very end with the ``buffer`` call^[This is largely inspired by Scala's [``SeqView``](http://www.scala-lang.org/api/current/index.html#scala.collection.SeqView).].

Another motivating reason for this design is precisely the immutability of delta objects. The stream could be forwarded directly to the client which may render the elements in the browser on-the-fly. A similar use case would be persistence, for example in an asynchronous database.

Scala's type refinements for traits come in useful. ``X`` takes ``changes``
from ``StateX``. It points to the same memory address in ``WriteX`` and ``DeltaX`` even though they are declared with different types. This is because ``Channel`` inherits both from ``WriteChannel`` and ``ReadChannel``.

The type-safety has an enormous benefit: A function can use a mutable stream internally, but returning the stream with writing capabilities would lead to unpredictable results. If the caller accidentally writes to this stream, this operation will succeed and in the worst case other subscribers receive the messages as well. As ``X`` inherits from ``ReadX``, the function can be more explicit and revoke some of its capabilities simply by returning ``ReadX[T]``. Similarly, if the caller *should* get writing capabilities and no read capabilities, this can be made explicit as well. This will make it trivial to find bugs related to reading and writing capabilities of streams directly during compile-time. And it makes interfaces more intelligible as a more specific type reduces the semantic space of a function.

The third advantage is correctness: With the functionality separated into different traits, the proper behaviour can be ensured using property-based testing. Rules for the generation of delta objects could be defined^[For example, a ``Delta.Clear`` may only be generated after ``Delta.Insert``.]. This stream is then used in ``StateX`` and all other traits can be tested whether they behave as expected. Presently, a very basic approach for property-based testing is implemented, but future versions will explore better ways to achieve a higher coverage.

A variety of generally applicable reactive operations were specified as traits in ``org.widok.reactive``. They can be seen as a contract and a reactive data structure should strive to implement as many as possible of these. Depending on conceptual differences, not every operation can be defined on a data structure, though. As the signatures are given, this ensures that all data structures use the operations consistently. Each of the traits group functions that are similar in their behaviour. Furthermore, the traits are combined into sub-packages which follow the properties mentioned at the beginning of the chapter, namely ``org.widok.reactive.{mutate, poll, stream}``.

To summarise, for a reactive data structure it is necessary to declare several traits with the following capabilities:

|           | **State** | **Mutation** | **Polling** | **Streaming** |
|-----------|-----------|--------------|-------------|---------------|
| ``Delta`` | no        | no           | no          | yes           |
| ``Poll``  | no        | no           | yes         | yes^[This is a practical decision. The ``Poll`` trait has direct access to the state. Thus, certain streaming operations can be implemented more efficiently. This should be avoided though as a delta stream would need to be persisted first in order for the ``Poll`` trait to be applicable. ]  |
| ``Read``  | no        | no           | yes         | yes           |
| ``Write`` | no        | yes          | no          | no            |
| ``State`` | yes       | no           | no          | no            |

: Traits and layers of a reactive data structure

## Reactive data structures
Widok currently implements three reactive data structures:

- **Channels:** single values like ``T``
- **Buffers:** lists like ``Seq[T]``
- **Dictionaries:**  maps like ``Map[A, B]``

## Channels
A channel is a multiplexer for typed messages of immutable values that it receives. Values sent to the channel get propagated to the observers that have been attached to the channel — in the same order as they were added.

Widok differentiates between two top-level channel types:

- **Channel:** corresponds to a reactive ``T``
- **Partial channel:** corresponds to a reactive ``Option[T]``

There are four channel implementations:

- ``Channel``: stream that does not persist its values
- ``Var``: variable stream; its value is always defined and has an initial value^[In Rx terms, ``Var`` would correspond to a *cold observer* as attaching to it will flush its current value. This is different from ``Channel`` which loses its messages when there are no subscribers.]
- ``LazyVar``: stream for lazily evaluated variables
- ``PtrVar``: stream for generic events^[It can be used to create delta channels from DOM variables by binding to the corresponding events that triggered by the value changes. For an example see ``Node.click``.]

Partial channels model optional values:

- ``PartialChannel``: base type
- ``Opt``: stream that has two states, either *defined with a value* or *undefined*

> **Note:** ``Opt[T]`` is merely a convenience type and ``Var[Option[T]]`` could be used, too.

### Examples
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

As the return values are channels themselves, chaining is possible.

Operations are FP methods such as ``map()``, ``filter()``or ``take()``. It must be noted that they have different semantics than their non-reactive counterparts. For brevity, only certain combinators are covered by the manual. For the rest, please refer to the [ScalaDoc documentation](http://widok.github.io/api/v0.2/index.html#org.widok.Channel).

> **TODO:** Rethink

A particular operator defined on channels is the addition. By adding up channels a new container channel is constructed. All values produced by the operands are propagated to this resulting channel and vice-versa (two-way propagation), but not amongst the operands:

```scala
val cont = ch + ch2  // more operands are possible, too
ch  := 42            // propagated to cont
ch2 := 23            // propagated to cont
cont := 65           // propagated to ch and ch2
```

An optional channel is constructed as follows:

```scala
val x = Opt[Int]()
x := 42
```

Alternatively, a default value may be passed:

```scala
val x = Opt(42)
```

To subscribe to the channel, use the method ``attach(f)``:

```scala
x.attach(println)
```

A higher-order operation only calls the passed function when the channel is listened to:

```scala
val ch = Var(42)
val f: Int => Int = _ + 1
val mapped = ch.map(f) // Does not call f
mapped.attach(println) // f(42)
mapped.attach(println) // f(42)
val mapped2 = mapped.map(f) // Does not call f
mapped.attach(println) // f(f(42))
```

This reduces the memory usage and complexity of the channel implementation as no caching is performed. On the other hand, you may want to perform on-site caching of the results of ``f``, especially if the function is side-effecting.

**TODO:** Update

The value that the state channel was instantiated with gets propagated upon attaching to the channel. A common use case are user interfaces. Usually, channels are set up before the widgets. The channels are then bound to the widgets. This renders the initial value directly in the DOM. Otherwise, it would be necessary to send the initial value to the channel manually as soon as the widget was rendered.

The following example visualises the difference in behaviour:

```scala
val ch = Var(42)
ch.attach(println) // prints 42

val ch2 = Channel[Int]()
ch2 := 42 // lost as ch2 does not have any observers
ch2.attach(println)
```

The argument is evaluated lazily and can also point to a mutable variable:

```scala
var counter = 0
val ch = Var(counter)
ch.attach(value => {counter += 1; println(value)}) // prints 0
ch.attach(value => {counter += 1; println(value)}) // prints 1
```

The following example illustrates a conceptual issue that arises with the use of chaining:

```scala
val ch = Var(42)
val ch2 = ch.map(_ + 1)
ch2.attach(...) // produces 43?
ch2.attach(...) // produces 43?
```

The question is whether initially produced values get propagated along the chain of operations.

### Child channels
**TODO:** Section outdated

In order to make all operations work properly on state channels, the notion of *child channels* was introduced. The previously given example is therefore well-defined. A child channel delays the propagation of the initial value until an observer is attached.

In order to figure out whether an operation supports this behaviour, it is sufficient to investigate its return type. The child channel behaviour only makes sense on operations that do not need more than one produced value to propagate it. A counterexample is ``skip()`` which conceptually must skip at least one element; the initial value will therefore never be propagated:

```scala
val ch = Var(42)
ch.skip(1) // returns Channel
  .attach(...)
```

> **Note:** Higher-order functions must not have any side-effects when used in operations on state channels.
>
> ```scala
> val ch = Var(42)
> ch.map(println).attach(_ => ())
> ```
>
> The example does not behave as expected and prints 42 two times instead of only once. This design decision was made consciously as to keep the implementation of operations more simple.

### Cached channels
**TODO:** Section outdated

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

## Buffers
Buffers are reactive lists:

```scala
val buf = Buffer(1, 2, 3)
buf.size.attach(println) // Prints 3, then 4
buf += 4
```

All polling methods have a dollar sign as suffix ``$``:

```scala
val buf = Buffer(1, 2, 3)
println(buf.size$) // Prints 3
buf += 4
```

A convenience class is ``RefBuf``. It wraps ``Ref[_]`` around each value and defines methods for easier insertion. Operations such as ``insertAfter(a, b)`` or ``removeAll(a)`` don't work with indices, but use the values to identify rows. This poses problems if a value is not guaranteed to be unique. ``Ref[_]`` makes each value unique. After construction the value cannot be changed. ``get`` will obtain the value. When using ``RefBuf`` in widgets, pattern matching is convenient to save a lot of ``get`` calls while having access to the reference:

```scala
val todos = RefBuf[Todo]()
ul().bind(todos) { case tr @ Ref(t) =>
  li(
    // Access value `t.completed`
    checkbox().bind(t.completed)

    // List operation remove() requires reference
  , button().onClick(_ => todos.remove(tr))
  )
}
```

> **TODO:** The following is outdated

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

## Dictionaries
Dictionaries are unordered maps from ``A`` to ``B``. Widok abbreviates the type as ``Dict``.

## User Interfaces
User interfaces (UIs) are notoriously heavily data-driven. Values do not only need to be displayed once, but continuously modified as the user interacts with the interface.

A higher interactivity results in more data dependencies and ultimately in more complex code. Imperative code in particular is prone to this shortcoming as dependencies must be modelled manually. With web application getting increasingly more interactive, solutions with clean and concise code are needed.

Widok advocates a flow-driven approach: Values are expressed in a stream-like data structure which we call *channel*. A channel is equipped with operations that allow to easily express data dependencies. Channels can have children to which the incoming values are propagated.

Built on top of channels, there is another data structure called *aggregates*. Channels model single values while aggregates represent a collection of values.

A key concept of channels and aggregates is that data may be rendered directly to the DOM without any intermediate caching. Whenever a new value is pushed to a channel, an atomic update takes place, only changing the associated DOM node. Similarly, this is respected when inserting, updating or removing items in an aggregate.

For an application that makes heavy use of data propagation, see our [TodoMVC implementation](https://github.com/widok/todomvc).

The proper functioning of each operation is backed by [test cases](https://github.com/widok/widok/tree/master/src/test/scala/org/widok). These serve as a complementary documentation as only a handful of operations are explained in full detail here.

Channels are a memory-efficient model and may be bound to widgets. The widget renders the received values on-the-fly. However, the lack of state restricts the possibilities. It may be desired to obtain the current value of a channel, change it or perform more elaborate operations that inherently require caching. This was taken into consideration and explicit caching can be performed.

### Binding to Widgets
A channel can be connected with one or more widgets. Most widgets provide two-way binding. To bind a channel to a widget, use the method ``bind()`` or its specialisations ``bindRaw()``, ``bindWidget()`` etc.

When associating a channel to multiple widgets, the contents amongst them is synchronised automatically:

```scala
val ch = Var("Hello world")
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
TODO This is not working anymore.

Although, channels and aggregates are used by widgets, they do not depend on JavaScript features. You can therefore debug their use directly on the console:

```bash
$ sbt console
import org.widok._
val ch = Channel[Int]()
ch.attach(println)
ch := 42
```

## Future Work
> **TODO:** Register event handler only in DOM when there are subscribers

## API documentation
- [Channel](http://widok.github.io/api/v0.2/index.html#org.widok.Channel)
- [Buffer](http://widok.github.io/api/v0.2/index.html#org.widok.Buffer)

