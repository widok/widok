package org.widok.bindings.FontAwesome

import org.widok._

trait Icon extends Widget[Icon] {
  def icon: String

  val rendered = DOM.createElement("span")
  css(s"fa", s"fa-$icon")

  def size(level: Int) = {
    assert((1 to 5).contains(level))
    css(s"fa-${level}x")
  }

  def stack(size: Int) = {
    assert((1 to 2).contains(size))
    css(s"fa-stack-${size}x")
  }

  def fixedWith(state: Boolean) = cssState(state, "fa-fw")
  def unorderedList(state: Boolean) = cssState(state, "fa-ul")
  def orderedList(state: Boolean) = cssState(state, "fa-li")
  def border(state: Boolean) = cssState(state, "fa-border")
  def spin(state: Boolean) = cssState(state, "fa-spin")
  def pulse(state: Boolean) = cssState(state, "fa-pulse")
  def rotate(deg: Int) = css(s"fa-rotate-$deg")
  def flipHorizontal(state: Boolean) = cssState(state, "fa-flip-horizontal")
  def flipVertical(state: Boolean) = cssState(state, "fa-flip-vertical")
  def inverse(state: Boolean) = cssState(state, "fa-inverse")
}
