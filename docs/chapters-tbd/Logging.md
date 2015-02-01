# Logging
There are four global logging methods:

* ``log()`` (Scala level: ``INFO``)
* ``error()`` (Scala level: ``SEVERE``)
* ``stub()`` (Scala level: ``WARNING``)
* ``trace()`` (Scala level: ``INFO``)

``log()`` and ``error()`` accept variable arguments with values to be logged.

## Production release
In order to keep debug messages in the code and filter these out only from from production releases, change the parameter of ``-Xelide-below`` in the settings to ``annotation.elidable.SEVERE``.