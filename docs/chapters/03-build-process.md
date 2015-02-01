# Build process
Widok uses sbt as its build system.

## Development releases
As with most programming languages, code optimisations are time-consuming and negligible during development. To compile the project without optimisations, run the following sbt command:

```bash
$ sbt fastOptJS
```

Note that prefixing ``~`` makes sbt constantly check for changed source files and only recompiles when needed.

This generates two files in ``target/scala-2.11/``:

- ``APPNAME-fastopt.js``
- ``APPNAME-launcher.js``

The former contains the whole project including its dependencies within a single JavaScript file, while the latter is a call to the entry point (see also chapter on [applications](Applications#Entry point)).

It is safe to concatenate these two files and ship them to the client.

## Production releases
Scala.JS uses the Google Closure Compiler to apply code optimisations. To create an optimised build, run:

```bash
$ sbt fullOptJS
```

Similarly as with ``fastOptJS``, you can also prefix ``~`` here.

TBD Explain how to have maintain sbt setups for production and development releases. This is necessary in order to have use a different value for ``-Xelidable-below``.

## Links
For more information on the build process, please refer to the [Scala.JS manual](http://www.scala-js.org/doc/sbt/run.html).

