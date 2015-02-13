# Bindings
## Bootstrap
Here is an example on how to use the Bootstrap bindings:

```scala
package org.widok.example.pages

import org.widok._
import org.widok.bindings.HTML
import org.widok.example.Routes
import org.widok.bindings.Bootstrap._

case class Main() extends Page {
  def header() =
    NavigationBar()(
      Container(
        NavigationBar.Header(
          NavigationBar.Toggle(),
          NavigationBar.Brand("Application name")),
        NavigationBar.Collapse(
          NavigationBar.Elements(
            NavigationBar.Leaf(Routes.notFound())(
              Glyphicon(Glyphicon.Search), "Page 1"),
            NavigationBar.Leaf(Routes.notFound())(
              Glyphicon(Glyphicon.Bookmark), "Page 2")),
          NavigationBar.Right(
            NavigationBar.Navigation(
              NavigationBar.Form(
                FormGroup(Role.Search)(
                  InputGroup(
                    Input.Text(placeholder = "Search query…")),
                  Button(Glyphicon.Search)())))))))

  def contents() = Seq(
    header(),
    Container(
      PageHeader(HTML.Heading.Level1("Page title ", HTML.Text.Small("Subtitle"))),
      Lead("Lead text"),
      "Page body"),
    Footer(Container(MutedText("Example page - All rights reserved."))))

  def ready(route: InstantiatedRoute) {}
}
```

As probably more than one page is going to use the same header, you should create a trait for it. You may also want to write a trait ``CustomPage`` which only requires you to define the page title and body in every page.

For the bindings to work, add the latest Bootstrap stylesheet to the ``head`` tag of your ``application.html`` file. If you want to use a CDN, use:

```html
<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.2.0/css/bootstrap.min.css">
```

