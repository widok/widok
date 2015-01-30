package org.widok.bindings.FontAwesome

import org.widok._

trait Icon extends Widget[Icon] {
  val icon: String

  val rendered = DOM.createElement("span")
  css(s"fa fa-$icon")

  def size(level: Int) = {
    assert((1 to 5).contains(level))
    css(s"fa-${level}x")
  }

  def fixedWith(state: Boolean) = {
    css(state, "fa-fw")
  }

  def unorderedList(state: Boolean) = {
    css(state, "fa-ul")
  }

  def orderedList(state: Boolean) = {
    css(state, "fa-li")
  }

  def border(state: Boolean) = {
    css(state, "fa-border")
  }

  def spin(state: Boolean) = {
    css(state, "fa-spin")
  }

  def pulse(state: Boolean) = {
    css(state, "fa-pulse")
  }

  def rotate(deg: Int) = {
    css(s"fa-rotate-$deg")
  }

  def flipHorizontal(state: Boolean) = {
    css(state, "fa-flip-horizontal")
  }

  def flipVertical(state: Boolean) = {
    css(state, "fa-flip-vertical")
  }

  def stack(size: Int) = {
    assert((1 to 2).contains(size))
    css(s"fa-stack-${size}x")
  }

  def inverse(state: Boolean) = {
    css(state, "fa-inverse")
  }
}
