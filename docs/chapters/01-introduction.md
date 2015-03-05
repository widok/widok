# Introduction
Widok is a reactive web framework for the JVM and Scala.js. Its key concepts are:

- **Pages:** Widok enforces modularisation of your web application. You split your application into *pages*. A *router* watches the browser query and loads the respective page.
- **Widgets:** The layout is specified in terms of composable widgets. Widgets are bound to dynamically changing values which are rendered on-the-fly in the browser.
- **Bindings:** Widok ships bindings for CSS frameworks like Bootstrap and Font-Awesome.
- **Reactive programming:** Reactive data structures, which implement a simple model of data propagation, are provided. Widok has reactive counterparts for variables, arrays, maps and sets. Instead of dealing with constant values, you specify the data flow as streams, on which you operate with higher-order functions like ``map()`` or ``filter()``.

## Comparison
In contrast to traditional web frameworks, a Widok application would implement the entire rendering logic and user interaction on the client-side. The sole purpose of the server would be to exchange data with the client. This approach leads to lower latencies and is more suitable for interactive applications.

Instead of writing HTML templates and doing manual DOM manipulations, Widok advocates widgets which are inspired by traditional GUI development. Widgets are first-class objects, allowing you to return them in functions. This can be useful when rendering a data stream consisting of widgets, or when you want to display a different widget depending on the device the client is using.

Another strength of Widok is that you can develop client-server applications entirely in Scala and CSS. Scala.js transpiles your code to JavaScript. Having only one implementation language reduces redundancy due to code sharing. This is especially useful for protocols. It also lets you develop safer web applications since you could use the same validation code on the client as on the server.

Widok is fully supported by IntelliJ IDEA. As Scala is a statically typed language you can use your IDE for refactoring and tab completion, increasing your productivity. Similarly, many bugs can be already identified during compile-time. Browser source maps will help you pinpoint run-time errors in the original source code. Scala.js supports continuous compilation which lets you iterate faster.

Finally, Widok is not necessarily bound to web applications. As it compiles to regular JavaScript code, you could develop [io.js](http://iojs.org/) applications or even native user interfaces with [NW.js](http://nwjs.io/). The JVM build comprises the reactive library, so that you can use it on the server-side as well.

