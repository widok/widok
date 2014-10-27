[![Build Status](https://travis-ci.org/widok/widok.svg)](https://travis-ci.org/widok/widok)

# Widok
Widok is a reactive web framework for Scala.js. Its key concepts are:

- **Page:** the browser query is dispatched to a page which renders widgets
- **Widget:** an element that corresponds to a node in the DOM
- **Channel:** a stream of values
- **Aggregate:** a channel container

## Comparison
Widok is different from traditional web frameworks in the following aspects:

- The rendering logic is implemented entirely on the client-side
- The sole purpose of the server is to exchange data with the client
- Instead of writing HTML templates, widgets are defined in pure Scala code
- Only widgets are allowed to manipulate the DOM
- Designed with memory-efficiency in mind
- Bootstrap 3 bindings available

As Widok is built around Scala.js it also inherits some of its properties:
- IDE support
- Browser source maps
- Fast compilation times

## Data propagation
Channels model continuous values as streams. These streams can be observed. Internally, no copies of the produced values are created. If desired, the current value can be explicitly cached, though. It is possible to operate on channels with higher-order functions such as ``map()`` and ``filter()``. Every time a new value is produced, it is propagated down the observer chain.

Aggregates are channel containers. They allow to deal with large lists efficiently. If an item gets added, removed or updated, this is reflected directly by a change in the DOM, only operating on the actual nodes.

## License
Widok is licensed under the terms of the GPLv3.

## Authors
* Tim Nieradzik

## Similar projects
* [scala.rx](https://github.com/lihaoyi/scala.rx)
* [scalatags](https://github.com/lihaoyi/scalatags)
* [monifu](https://github.com/monifu/monifu)

## Links
* [Project page](https://widok.github.io/)
* [Source code](https://github.com/widok/widok)
* [User manual](https://github.com/widok/widok/wiki)
* [API documentation](http://widok.github.io/api/latest/)
* [Issue tracker](https://github.com/widok/widok/issues)
* [Travis CI](https://travis-ci.org/widok/widok)