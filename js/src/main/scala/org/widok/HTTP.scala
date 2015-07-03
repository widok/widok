package org.widok

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

import org.scalajs.dom.ext.Ajax

/**
 * Wrapper around Scala.js Ajax class to send GET,POST,PUT and DELETE
 * request to a url
 */
object HTTP {

  def get(url: String, requestHeaders: Map[String, String] = Map.empty) =
    Ajax.get(url = url, headers = requestHeaders).map ( _.responseText )

  def post(url: String, data: String, requestHeaders: Map[String, String] = Map.empty) =
    Ajax.post(url = url, data = data, headers = requestHeaders).map ( _.responseText )

  def put(url: String, data: String, requestHeaders: Map[String, String] = Map.empty) =
    Ajax.put(url = url, data = data, headers = requestHeaders).map ( _.responseText )

  def delete(url: String, data: String, requestHeaders: Map[String, String] = Map.empty) =
    Ajax.delete(url = url, data = data, headers = requestHeaders).map ( _.responseText )

}

