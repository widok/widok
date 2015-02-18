package org.widok.bindings

import org.widok._

package object FontAwesome {
  case class Stack(contents: Icon*) extends Widget[Stack] {
    val rendered = DOM.createElement("span", contents)
    css("fa-stack")

    def large(state: Boolean) = {
      cssState(state, "fa-lg")
    }
  }
}
