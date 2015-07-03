package org.widok

import org.scalajs.dom._
import scala.collection.immutable.HashMap

case class SaveMany(kvp: HashMap[String, String])

/**
 * wrapper around window.localStorage provides some additional methods.
 */
object LocalStorage {

  private val storage = window.localStorage

  def save(key: String, value: String = ""): Unit = storage.setItem(key, value)

  def save(saveMany: SaveMany): Unit = saveMany.kvp.foreach(kvp => save(kvp._1, kvp._2))

  def get(key: String) = storage.getItem(key)

  def removeItem(key: String) = storage.removeItem(key)

}
