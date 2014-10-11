[![Build Status](https://travis-ci.org/widok/widok.svg)](https://travis-ci.org/widok/widok)

# Widok
Widok is a reactive web framework for Scala.JS. Its key concepts are:

- **Page:** dispatches the current query to a page which renders widgets
- **Widget:** element to be rendered by the browser
- **Channel:** value stream; for 1:n data propagation
- **Aggregate:** container of channels; for n:m data propagation

## Comparison
Widok is different from traditional web frameworks in the following aspects:

- The rendering logic is implemented entirely on the client-side
- The sole purpose of the server is to exchange data with the client
- Instead of writing HTML templates, widgets are defined in pure Scala code
- Only widgets are allowed to manipulate the DOM
- Designed with memory-efficiency in mind
- Bootstrap 3 bindings available

As Widok is built around Scala.JS it also inherits some of its properties:
- IDE support
- Browser source maps
- Fast compilation times

## Widgets
HTML elements are provided as widgets. Also, native Bootstrap 3 bindings exist to allow for faster prototyping.

## Data propagation
Channels model realtime values as a stream. This stream can be observed. Internally, no copies of the produced values are created. If desired, the current value can be explicitly cached, though. It is possible to operate on channels with higher-order functions. Every time a new value is produced, it is propagated down the observer chain.

Aggregates are channel containers. They allow to deal with large lists efficiently. As aggregates may be rendered directly to the browser, no copies of the list are being made. If an item gets removed, this is reflected directly by a change in the DOM, only deleting the actual node.

## License
Widok is licensed under the terms of the GPLv3.

## Authors
Tim Nieradzik <tim@kognit.io>

## Links
* [User manual](https://github.com/widok/widok/wiki)
* [Project page](https://widok.github.io/)
* [API documentation](http://widok.github.io/api/index.html)
* [Source code](https://github.com/widok/widok)
* [Travis CI](https://travis-ci.org/widok/widok)
