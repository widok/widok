# Developing
If you would like to participate or try out development releases, please read this chapter.

## API
Widok is still in its early stages and the API may be subject to changes. Any recommendations for improvements are welcome.

## Compilation
To work on the development version of Widok, run the following commands:

```bash
$ git clone git@github.com:widok/widok.git
$ cd widok
$ sbt publish-local
```

This compiles the latest version of Widok and installs it locally. To use it, make sure to also update the version in your project accordingly.

## Releases
The versioning scheme follows the format `releaseSeries.relativeVersion`. Thus, v0.2.0 defines the version 0 of the release series 0.2. All versions within the same release series must be binary-compatible. If any of the dependencies (like Scala.js) are updated, the release series must be increased as well.

The latest version of Widok is always published to [Maven Central](https://search.maven.org/).

## Manual
Since v0.2, the manual is stored in the same repository as the code. This enables you to commit code with the corresponding documentation changes. At any time, the manual should always reflect the current state of the code base.

