# Validation

Form validation can be done by initializing a Validator with a number of Tuple2[ReadStateChannel[_], Seq[FieldValidation[_]]].

```scala

package example

import org.widok._
import org.widok.html._
import org.widok.validation._
import org.widok.validation.Validations._

case class TestPage() extends Page {

  val username = Var("")
  val password = Var("")
  val passwordVerify = Var("")
  val samePasswords = password.combine(passwordVerify).cache(("", ""))

  val patternValidation = PatternValidation(".{3,6}")

  val validator = Validator(
    username -> Seq(RequiredValidation(), EmailValidation()),
    password -> Seq(RequiredValidation(), MinLengthValidation(5)),
    samePasswords -> Seq(SameValidation())
  )

  override def view(): View = div(
    form(
      label("Username").forId("username"),
      text().id("username").bind(username),

      // display the validation error messages provided by the failing validation(s)
      validator.errors.value(username).values.map {
        case None => div()
        case Some(v) => div(s"Validation error: ${v.mkString(", ")}")
      },

      // set "invalid" css class on password fields when validation fails
      label("Password").forId("password"),
      text().id("password").bind(password).cssState(validator.invalid(password), "invalid"),

      label("Repeat password").forId("passwordVerify"),
      text().id("passwordVerify").bind(passwordVerify).cssState(validator.invalid(passwordVerify), "invalid"),

      // show span when passwords differs
      span("Passwords must match").show(validator.invalid(samePasswords)),

      // only enabled when form is valid.
      // call validate so that validation is triggered for any non-dirty fields
      button("Register").enabled(validator.valid).onClick(_ => if (validator.validate()) signup())
    )
  )

  def signup() = {}

  override def ready(route: InstantiatedRoute): Unit = {}
}
```