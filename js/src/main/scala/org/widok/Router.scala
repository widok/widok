package org.widok

import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global

import org.scalajs.dom

import scala.scalajs.js.URIUtils

case class Route(path: String, page: () => Page) extends Ordered[Route] {
  val routeParts = path.split('/')

  def compare(that: Route): Int =
    if (routeParts.length != that.routeParts.length)
      routeParts.length.compareTo(that.routeParts.length)
    else routeParts.zip(that.routeParts).foldLeft(0) { (acc, cur) =>
      if (acc != 0) acc
      else cur match {
        case (left, right) =>
          if (left.startsWith(":") && !right.startsWith(":")) -1
          else if (!left.startsWith(":") && right.startsWith(":")) 1
          else left.compareTo(right)
      }
    }

  /** Instantiates route with multiple arguments */
  def apply(args: Map[String, String] = Map.empty): InstantiatedRoute =
    InstantiatedRoute(this, args)

  /** Instantiates route with only one argument */
  def apply(param: String, arg: String): InstantiatedRoute =
    InstantiatedRoute(this, Map(param -> arg))

  def matches(queryParts: Seq[String]): Boolean =
    if (routeParts.length != queryParts.length) false
    else routeParts.zip(queryParts).forall{
      case (rt, qry) if rt == qry => true
      case (rt, qry) if rt.startsWith(":") => true
      case _ => false
    }

  def parseArguments(queryParts: Seq[String]): Map[String, String] =
    routeParts.zip(queryParts).foldLeft(Map.empty[String, String]) { (acc, cur) =>
      val (key, value) = cur
      if (key.startsWith(":")) acc + (key.substring(1) -> value)
      else acc
    }
}

// TODO Find better name
case class InstantiatedRoute(route: Route, args: Map[String, String] = Map.empty) {
  def query(): String = {
    val result = args.foldLeft(route.path) { (acc, cur) =>
      val (key, value) = cur
      val replace = s":$key"
      assume(acc.contains(replace), s"Route contains named parameter '$key'")
      acc.replace(replace, Router.encodePart(value))
    }

    assume(!result.contains(":"), "All parameters were set")
    result
  }

  def uri(): String = "#" + query()

  /**
   * Dispatches a route by changing the current hash. If the hash did not
   * change, setting it would probably not result in the `hashchange` event
   * being triggered. Therefore, render the page manually.
   */
  def go() {
    val target = uri()

    if (dom.window.location.hash == target) log("[router] Hash not changed")
    else {
      log("[router] Location changed; changing browser hash")
      dom.window.location.hash = target
    }
  }
}

case class Router(unorderedRoutes: Set[Route],
                  startPath: String = "/",
                  fallback: Option[Route] = None)
{
  val currentPage = Opt[Page]()

  /** Checks whether no two elements in `unorderedRoutes` are symmetric. */
  assume((for {
    x <- unorderedRoutes
    y <- unorderedRoutes.filter(_ != x)
  } yield x.compare(y)).forall(_ != 0), "All routes are distinguishable")

  val routes = unorderedRoutes.toSeq.sorted.reverse

  def matchingRoute(queryParts: Seq[String]) =
    routes.find(_.matches(queryParts))

  /** Starts listening on the hash change event and triggers routes. */
  def listen() {
    dom.window.onhashchange = { (e: dom.HashChangeEvent) =>
      dispatchPath(e.newURL,
        if (e.oldURL == "") None
        else Some(e.oldURL))
    }

    if (dom.window.location.hash.isEmpty) dom.window.location.hash = startPath
    else dispatchPath(dom.window.location.hash)
  }

  /**
   * Dispatches route if path changed
   */
  def dispatchPath(nextPath: String, prevPath: Option[String] = None) {
    if (!prevPath.contains(nextPath)) dispatchQuery(nextPath)
    else log("[router] No action as query did not change")
  }

  def render(nextPage: Page, route: InstantiatedRoute) {
    nextPage.render(route).onComplete {
      case Success(r) =>
        currentPage.get.foreach(_.destroy())
        PageContainer.replace(r)
        nextPage.ready(PageContainer.node)
        currentPage := Some(nextPage)

      case Failure(t) => error(s"Failed to load page: $t")
    }
  }

  def dispatchQuery(query: String) {
    log(s"[router] Dispatching query $query")
    val queryParts = Router.queryParts(query)

    matchingRoute(queryParts) match {
      case Some(route) =>
        log(s"[router] Found $route")
        val args = route.parseArguments(queryParts)
        render(route.page(), InstantiatedRoute(route, args))

      case _ =>
        error("[router] Choosing fallback route")
        fallback match {
          case Some(fb) => render(fb.page(), InstantiatedRoute(fb))
          case None => error("[router] No route found")
        }
    }
  }
}

object Router {
  def decodePart(argument: String): String = URIUtils.decodeURIComponent(argument)
  def encodePart(argument: String): String = URIUtils.encodeURIComponent(argument)

  def queryParts(uri: String): Seq[String] =
    Helpers.after(uri, '#')
      .flatMap {
        case x if x.isEmpty => None
        case x => Some(x.split('/').toSeq.map(decodePart))
      }
      .getOrElse(Seq.empty)
}