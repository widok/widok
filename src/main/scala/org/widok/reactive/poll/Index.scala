package org.widok.reactive.poll

/** Query by index */
trait Index[Container[_], Index, T] {
  /** Index of `handle` */
  def indexOf(handle: T): Index

  /** All rows as a native type */
  def get: Container[T]

  /** N-th row */
  def value(index: Index): T

  /** @see [[value]] */
  def apply(index: Index): T = value(index)
}
