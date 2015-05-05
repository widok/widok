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
  val samePasswords = password.combine(passwordVerify).cache("", "")

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
      
      // TODO: the map operation on the validator.error.value(ch) will not remove the rendered div when the validation
      //       goes from failed -> ok. This causes the key (channel) to be removed from the validation.errors dict.
      //       Is it a bug? Shouldn't the rendered error message be removed automatically in this case?
      //       The "fix" in this case is to add a .show(...) statement that will only show the error message when there
      //       is a reported error for the field.
      validator.errors.value(username).map(e => div("Validation errror: " + e.mkString(", ")).show(validator.errors.value(username).isDefined)),

      // set "invalid" css class on password fields when validation fails
      label("Password").forId("password"),
      text().id("password").bind(password).cssState(validator.validations.value(password).isDefined, "invalid"),

      label("Repeat password").forId("passwordVerify"),
      text().id("passwordVerify").bind(passwordVerify).cssState(validator.validations.value(password).isDefined, "invalid"),

      // show span when passwords differs
      span("Passwords must match").show(validator.errors.value(samePasswords).isDefined),

      // only enabled when form is valid.
      // call validate so that validation is triggered for any non-dirty fields
      button("Register").enabled(validator.valid).onClick(_ => if (validator.validate()) signup())
    )
  )
  
  def signup() = {}

  override def ready(route: InstantiatedRoute): Unit = {}
}
```