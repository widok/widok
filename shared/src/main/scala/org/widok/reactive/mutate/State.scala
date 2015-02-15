package org.widok.reactive.mutate

trait State[T] {
  /**
   * @note Buffers: Change each row to the value returned by `f`
   * @note State channels: Change current value (if exists) according to `f`
   */
  def update(f: T => T)
}
