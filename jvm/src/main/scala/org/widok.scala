package org

import org.widok.{ Channel, WriteChannel }

package object widok {
  implicit def FunctionToWriteChannel[T](f: T => Unit): WriteChannel[T] = {
    val ch = Channel[T]()
    ch.attach(f)
    ch
  }
}