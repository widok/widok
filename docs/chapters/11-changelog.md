# Changelog
The changelog lists all major changes between releases. For more details, please see the [Git changelog](https://github.com/widok/widok/commits/master).

## Version 0.2.1
### General work
- Ported to Scala.js v0.6.2
- Use io.js for running the test cases

### Widgets
- Allow the use of ``Buffer.map()`` in ``tbody``
- Provide ``autofocus()`` and ``placeholder()`` on Textarea
- Widget: Add ``+=`` and ``-=``
- Widget: Support drag events
- Widget: Don't wrap ``ReadChannel[Widget[_]]`` in spans
- Widget: Add ``focus()``

### Event propagation
- Opt: Fix ``undefined$``
- Dict: Fix ``size``
- Dict: Generate fine-grained delta objects in ``toBuffer``
- Dict: Add ``sortBy()``
- Buffer: Add ``flatMapSeq()`` (in consistency with Channel)

### Bindings
- Bootstrap: Implement AnchorButton
- Bootstrap: Implement Number
- Bootstrap: Implement Textarea
- Bootstrap: Implement Radio
- Bootstrap: Add argument to Checkbox
- Bootstrap: Fix positioning of Typeahead wrt. scrolling
- Bootstrap: Backport upstream changes to fix scrolling behaviour in modals
- Bootstrap: Remove ``TextContainer``, use ``textStyle()`` instead

### Routing
- Support percent-encoded arguments

## Version 0.2.0
### General work
- Relicensed as Apache v2.0
- Ported to Scala 2.11.5
- Ported to Scala.js v0.6.0
- Ported to scalajs-dom v0.8.0
- Uses [minitest](http://github.com/monifu/minitest) for the test cases
- [Shapeless](https://github.com/milessabin/shapeless) dependency dropped; the lens macros resulted in slower compilation and had insufficient IDE support. At the time of writing, Shapeless was not yet ported to Scala.js v0.6.0. ``Var()`` can be used as a drop-in replacement for more flexibility and better performance. As a result of dropping Shapeless, the Sonatype resolver is not needed anymore.
- There is a [Gitter channel](https://gitter.im/widok/widok) for conversations about Widok
- JVM support: Widok is now built for the JVM, allowing you to use the reactive library on the server, too. Upcoming versions will further focus on network transparency (see [#26](https://github.com/widok/widok/issues/26)). In the future, the widget library will be usable under the JVM as well (see [#25](https://github.com/widok/widok/issues/25)).
- The manual is now part of the code repository. Pandoc is used for compilation. Current targets are PDF, HTML and EPUB.

### Event propagation
This version includes a complete redesign of the event propagation mechanisms. The previous implementation was merely a proof of concept and therefore had a couple of design issues. Changes include:

- More abstraction layers for better type-safety and modularity. The whole API now strictly distinguishes between ``ReadChannels`` and ``WriteChannels``. By looking at the types of a function, it is now more predictable what the passed channel is being used for.
- Improve naming: ``Var`` and ``Buffer`` as opposed to ``CachedChannel`` and ``CachedAggregate``
- ``Aggregate``s were dropped. Instead, reactive data structures were introduced, which encode their changes as delta objects and are built on top of channels. Publishing changes as channels allows to persist the stream or send it directly to the client. Furthermore, change transformers could be written, for example one that cancels out common sequences like for better rendering performance:

	```scala
	Change.Insert(Position.Last(), element)
	Change.Remove(element)
	```
- Reactive data structures for buffers, dictionaries and sets were introduced.
- A reactive Rose tree implementation was added.
- Back propagation for aggregates was entirely removed. For sequences it offered little benefit. Previously, back propagation was only used for deletions. Abandoning this feature makes it easier to implement new combinators. For example, ``filter()`` became significantly shorter and better to comprehend.
- Partially defined streams were introduced (``Opt``)
- Created traits that fully specify the functionality of reactive combinators (see ``Combinators.scala``). As most combinators are implemented by more than one class, this will ensure consistency in their usage. They could also be used outside of Widok as the basis for alternative implementations.
- Work on resource management has begun; ``dispose()`` can be called on channels to clear its subscription in the parent.
- Property-based testing for channels and aggregates (basic operations only)
- Cycle detection for channels. As cycles may be well-defined under certain circumstances, they can be ignored using ``filterCycles`` (see the [TodoMVC application](https://github.com/widok/todomvc) for an example)

### Widgets
Some work also went into the widget subsystem:

- New implicits to render numerical values, buffers and sequences without type conversions
- DOM event listeners are now available on all widgets and made available as channels. This implies that more than one listener can be attached to the same event. Also, ``on*()`` short-hands were created for all events. This makes registering a click event listeners as simple as writing ``.onClick(ev => ...)``.
- All DOM elements have short aliases which are equal to their HTML tag names. Instead of ``Input.Text()``, you could now write ``text()``. As these tag names may conflict in certain cases, an explicit import is necessary: ``import org.widok.html._``
- The Bootstrap bindings were completely revised. No type casts are necessary anymore and most widgets are now ``case class``es instead of functions. This makes the widgets easier in their usage. The bindings now cover a large percentage of the functionality Bootstrap provides.

Newly added widgets are:

- Bootstrap: Modal dialogue
- Bootstrap: Typeahead field
- Bootstrap: Form validation
- Bootstrap: Table
- Bootstrap: Pagination
- Bootstrap: Panel
- Bootstrap: Button group and toolbar
- Bootstrap: Breadcrumb
- Bootstrap: Media object
- HTML: File input
- HTML: Password field
- HTML: Select box
- HTML: Radio button
- HTML: HTML5 number input field
- Widget container ``Inline()`` (can be used when a ``span()`` would introduce undesired design glitches)
- Image placeholder
- Lorem ipsum

Code generators were introduced for higher reliability of the bindings. [sbt-web](https://github.com/sbt/sbt-web) is used internally to obtain external web dependencies. As part of the build process, Scala files are then created. Auto-generated bindings are provided for:

- Bootstrap's glyphicons
- Font-Awesome

### Routing
- In v0.1 the DOM nodes of all routes were initialised when the page loads. This may result in cycle errors. Now, a route change also reinitialises the entire page.
- The method ``destroy()`` can be overridden to react on page changes, for example for manual resource management

### Trivia
- The API changes significantly shrinked TodoMVC's file size from 3396 bytes (92 lines) to 3059 bytes (89 lines).
- Widok can be used to develop desktop applications with [NW.js](https://github.com/nwjs/nw.js/) as shown by [poliglot-ui](http://github.com/poliglot/poliglot-ui)

