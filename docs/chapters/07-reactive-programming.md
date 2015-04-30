# Reactive programming
## Motivation
User interfaces are heavily data-driven. Values do not only need to be displayed once, but continuously modified as the user interacts with the interface. Interactivity requires data dependencies which ultimately lead to deeply intertwined code. Imperative code in particular is prone to this shortcoming since dependencies are hard to express. As web applications are increasingly more interactive, a flow-driven approach is desirable. Focussing on flows, the essence of the program will be to specify the data dependencies and how values propagate to the user interface and back.

To tackle this issue, Widok follows a reactive approach. Consider an application to visualise stock market data. You are listening to a real-time stream producing values. Then, you want to display only the most current stock price to the user. This is solved by creating a 'container' which is bound to a DOM node. Whenever you feed a new stock price to it, an atomic update takes place in the browser, only changing the value of the associated DOM node.

Another example is a monitoring service which allows you to control on-the-fly the log level of a web application. A select box will list all possible log levels, like ``debug`` or ``critical``. When the page is first loaded, it obtains the current log level from the server. Changing its value, however, must back-propagate and send the selection to the server. All other clients that are connected are notified of the change as well.

For a simple application that illustrates client-side data propagation, see our [TodoMVC implementation](https://github.com/widok/todomvc).

## Concepts
Reactive programming is a paradigm that focuses on:

a) propagation of data, specifically changes, and
b) data flow.

Concretely, a data structure is said to be *reactive* (or *streaming*) if it models its state as streams. It does this by defining change objects (*deltas*) and mapping its operations onto these. The published stream is read-only and can be subscribed. If the stream does not have any subscribers, the state would not get persisted and is lost.

> **Example:** A reactive list implementation could map all its operations like ``clear()`` or ``insertAfter()`` on the two delta types ``Insert`` and ``Delete``. A subscriber can interpret the deltas and persist the computed list in an in-memory buffer.

Another property of a reactive data structure is that it does not only stream deltas, but also state observations. Sticking to the reactive list example, the deltas could allow streaming observations on the list's inherent properties — one being the length, another the existence of a certain element, i.e. ``contains(value)``.

Finally, a *mutable* reactive data structure is an extension with the sole difference that it maintains an internal state which always represents the computed result after a delta was received. This is a hybrid solution bridging mutable object-oriented objects with reactive data structures. The mutable variant of our reactive list could send its current state when a subscriber is registering. This ultimately leads to better legibility of code as subscribers can register at any point without caring whether the expected data has been propagated already. The second reason is that otherwise we would need multiple instances of mutual objects that interpret the deltas. This is often undesired as having multiple such instances incurs a memory overhead.

To recap, a reactive data structure has four layers:

- **State:** interpretation of the delta stream and "converting" it into a mutable object
- **Mutation operations:** functions to produce deltas on the stream^[These functions do not access the state in any way.]
- **Polling operations:** blocking functions to query the state
- **Streaming operations:** publish the state changes as a stream

Obviously, the first three layers are the very foundation of object-orientation. It is different in that a) modifications are encoded as deltas and b) there are streaming operations.

For now we just covered the first component of reactive programming: data propagation. The second cornerstone, data flow, is equally important, though. Streams describe data flow in terms of dependencies. Considering you want to plot a line as a graph using the formula $y = mx+b$ and the user provides the values for $m$ and $b$, then you would wrap these inputs in channels and express the dependencies using combinators^[The types in the code only serve illustration purposes]:

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

The user could listen to ``y`` and whenever it receives a new function, it can just call it for all the ``x`` in the interval of the shown graph. The example shows that messages in streams are not bound to data objects and even immutable functions could be passed around.

The data propagation is illustrated by the following diagram:

![Change propagation for $y=mx+b$](images/data-flow.pdf)

As soon as the user inserts a value for ``m`` as well as ``b``, ``mAndB`` will produce a tuple. Then, ``y`` computes the final function.

How channels work in detail is explained in the following sections. This example should only give an intuition of the fundamental concepts and how data dependencies are expressed.

## Requirements
The term "stream" was used several times. This term is polysemous and requires further explanation. In reactive programming there are different types of streams with severe semantic differences.

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

To better differentiate from the established reactive frameworks, a less biased term than *observable* was called for and the reactive streams are therefore called *channels* in Widok. The requirements have been implemented as follows: A subscriber is just a function pointer (wrapped around a small object). A channel can have an unlimited number of children whereas each of the child channels knows their parent. A function for flushing the content of a channel upon subscription can be freely defined during instantiation^[This function is called by ``attach()`` and produces multiple values which is necessary for some reactive data structures like lists.]. When a channel is destroyed, so are its children. Error handling is not part of the implementation. Similarly, no back-pressure is performed, but back-propagation is implemented for some selected operations like ``biMap()``.

For client-side web development only a small percentage of the interaction with streams require the features observables provide and this does not justify a more complex overall design. It is possible to use a full-fledged model like Rx or Monifu for just those areas of the application where necessary by redirecting (piping) the channel output.

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
Widok currently implements four reactive data structures:

- **Channels:** single values like ``T``
- **Buffers:** lists like ``Seq[T]``
- **Dictionaries:**  maps like ``Map[A, B]``
- **Sets:**  reactive ``Set[T]``

## Channels
A channel models continuous values as a stream. It serves as a multiplexer for typed messages that consist of immutable values. Messages sent to the channel get propagated to the observers that have been attached to the channel — in the same order as they were added. It is possible to operate on channels with higher-order functions such as ``map()``, ``filter()`` or ``take()``. These methods may be chained, such that every produced values is propagated down the observer chain.

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

### Operations
Here is a simple example for a channel that receives integers. We register an observer which prints all values on the console:

```scala
val ch = Channel[Int]()  // initialise
ch.attach(println)       // attach observer
ch := 42                 // produce value
```

> **Note:** The ``:=`` operator is a shortcut for the method ``produce``.

The return values of operations are channels, therefore chaining is possible. Channels can be used to express data dependencies:

```scala
val ch = Channel[Int]()
ch.filter(_ > 3)
  .map(_ + 1)
  .attach(println)
ch := 42  // 43 printed
ch := 1   // nothing printed
```

Use the method ``distinct`` to produce a value if it is the first or different from the previous one. A use case is to perform time-consuming operations such as performing HTTP requests only once for the same user input:

```scala
ch.distinct.attach { query =>
  // perform HTTP request
}
```

Considering that you want to observe multiple channels of the same type and merge the produced values, you can use the ``|`` operator^[It is an alias for the method ``merge()``]:

```scala
val a = Channel[String]()
val b = Channel[String]()
val c = Channel[String]()

val merged: ReadChannel[String] = a | b | c
```

It must be noted that streaming operations have different semantics than their non-reactive counterparts. For brevity, only certain combinators are covered by the manual. For the rest, please refer to the ScalaDoc documentation.

### State channels
For better performance, ``Channel`` does not cache the produced values. Some operations cannot be implemented without access to the current value, though. And often it is necessary to poll the current value. For these reasons *state channels* such as ``Var`` or ``Opt`` were introduced. The following example visualises the different behaviours:

```scala
val ch = Var(42)
ch.attach(println)  // prints 42

val ch2 = Channel[Int]()
ch2 := 42  // Value is lost as ch2 does not have any observers
ch2.attach(println)
```

``update()`` is an operation that requires that the produced values are persisted. ``update()`` takes a function which modifies the current value:

```scala
val ch = Var(2)
ch.attach(println)
ch.update(_ + 1)  // produces 3
```

A partially-defined channel (``Opt``) is constructed as follows:

```scala
val x = Opt[Int]()
x := 42
```

Alternatively, a default value may be passed:

```scala
val x = Opt(42)
```

A state channel provides all the methods a channel does. ``Var[T]`` and ``Opt[T]`` can be obtained from any existing ``ReadChannel[T]`` using the method ``cache``:

```scala
val chOpt = ch.cache      // Opt[Int]
val chVar = ch.cache(42)  // Var[Int]
```

``chOpt`` is undefined as long as no value was produced on ``ch``. ``chVar`` will be initialised with 42 and the value is overridden with the first produced value on ``ch``.

``biMap()`` allows to implement a bi-directional map, i.e. a stream with back-propagation:

```scala
val map   = Map(1 -> "one", 2 -> "two", 3 -> "three")
val id    = Var(2)
val idMap = id.biMap(
  (id: Int)     => map(id)
, (str: String) => map.find(_._2 == str).get._1)
id   .attach(x => println("id   : " + x))
idMap.attach(x => println("idMap: " + x))
idMap := "three"
```

The output is:

```
id   : 2
idMap: two
id   : 3
idMap: three
```

``biMap()`` can be used to implement a lens as a channel. The following example defines a lens for the field ``b``. It has a back channel that composes a new object with the changed field value.

```scala
case class Test(a: Int, b: Int)
val test = Var(Test(1, 2))
val lens = test.biMap(_.b, (x: Int) => test.get.copy(b = x))
test.attach(println)
lens := 42  // produces Test(1, 42)
```

A ``LazyVar`` evaluates its argument lazily. In the following example, it points to a mutable variable:

```scala
var counter = 0
val ch = LazyVar(counter)
ch.attach(value => { counter += 1; println(value) })  // prints 0
ch.attach(value => { counter += 1; println(value) })  // prints 1
```

### Call semantics
Functions passed to higher-order operations are evaluated on-demand:

```scala
val ch = Var(42).map(i => { println(i); i + 1 })
ch.attach(_ => ())  // prints 42
ch.attach(_ => ())  // prints 42
```

The value of a state channel gets propagated to a child when it requests the value (``flush()``). In the example, ``Var`` delays the propagation of the initial value 42 until the first ``attach()`` call. ``attach()`` goes up the channel chain and triggers the flush on each channel. In other words, ``map(f)`` merely registers an observer, but doesn't call ``f`` right away. ``f`` is called each time when any of its direct or indirect children uses ``attach()``.

This reduces the memory usage and complexity of the channel implementation as no caching needs to be performed. On the other hand, you may want to perform on-site caching of the results of ``f``, especially if the function is side-effecting.

The current value of a state channel may be read at any time using ``.get`` (if available) or ``flush()``.

There are operations that maintain state for all observers. For example, ``skip(n)`` counts the number of produced values^[``n`` must be greater than 0.]. As soon as ``n`` is exceeded, all subsequent values are passed on. The initial ``attach()`` calls ignore the first value (42), but deal with all values after that:

```scala
val ch = Var(42)
val dch = ch.drop(1)
dch.attach(println)
dch.attach(println)
ch := 23  // produces 23 twice
```

### Cycles
Certain propagation flows may lead to cycles:

```scala
val todo = Channel[String]()
todo.attach { t =>
    println(t)
    todo := ""
}
todo := "42"
```

Setting ``todo`` will result in an infinite loop. Such flows are detected and will lead to a run-time exception. Otherwise, the application would block indefinitely which makes debugging more difficult.

If a cycle as in the above example is expected, use the combinator ``filterCycles`` to make it explicit. This will ignore value propagations caused by a cycle.

## Buffers
Buffers are reactive lists. State changes such as row additions, updates or removals are encoded as delta objects. This allows to reflect these changes directly in the DOM, without having to re-render the entire list. ``Buffer[T]`` is therefore more efficient than ``Channel[Seq[T]]`` when dealing with list changes.

The following example creates a buffer with three initial rows, observes the size^[``size`` returns a ``ReadChannel[Int]``.] and then adds another row:

```scala
val buf = Buffer(1, 2, 3)
buf.size.attach(println) // Prints 3
buf += 4  // Inserts row 4, prints 4
```

All polling methods have a dollar sign as suffix ``$``:

```scala
val buf = Buffer(1, 2, 3)
println(buf.size$) // Prints 3
```

An example of using ``removeAll()``:

```scala
val buf  = Buffer(3, 4, 5)
val mod2 = buf.filter$(_ % 2 == 0)

buf.removeAll(mod2.get)
```


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

> The value of a ``Ref[_]`` can be obtained by calling ``get``. However, it is more convenient to do pattern matching as in the example.

You can observe the delta objects produced by a buffer:

```scala
val buf = Buffer(1, 2, 3)
buf.changes.attach(println)
buf += 4
buf.clear()
```

This prints:

```
Insert(Last(),1)
Insert(Last(),2)
Insert(Last(),3)
Insert(Last(),4)
Clear()
```

All streaming operations that a buffer provides are implemented in terms of the ``changes`` channel.

## Dictionaries
Dictionaries are unordered maps from ``A`` to ``B``. Widok abbreviates the type as ``Dict``.

## Sets
Reactive sets are implemented as ``BufSet``^[This name was chosen as ``Set`` would have collided with Scala's implementation.].

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

## Tests
The proper functioning of each operation is backed by [test cases](https://github.com/widok/widok/tree/master/shared/src/test/scala/org/widok). These provide complementary documentation.

