# Bindings
This chapter deals with foreign CSS frameworks which Widok provides typed bindings for.

## Bootstrap
### External stylesheet
For the bindings to work without sbt-web, add the latest Bootstrap stylesheet to the ``head`` tag of your ``application.html`` file. You can either keep a copy of the stylesheet locally or use a CDN:

```html
<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.2/css/bootstrap.min.css">
```

Please keep in mind that the pre-built stylesheet comes with certain restrictions, like the font path being hard-coded.

### Navigation bar
Here is an example on how to use the ``NavigationBar`` widget:

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

### Glyphicons
...

### Forms
...

## Font-Awesome
The bindings include all icons in camel-case notation. For convenient usage, rename it when you import the object:

```scala
import org.widok.bindings.{FontAwesome => fa}
```

Using the [``user`` icon](http://fortawesome.github.io/Font-Awesome/icon/user/) is as simple as writing:

```scala
fa.User()
```

This translates to:

```html
<span class="fa fa-user"></span>
```

