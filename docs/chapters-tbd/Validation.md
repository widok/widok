# Validator

Generic client-side dynamic form field validation can be managed by the Validator class. It is constructed with a number
of tupled (ReadChannels, Seq(Validations)). When data is read from a channel it is validated against all associated
Validations.

## Validation channels

The Validator exposes the following channels that can be used in widgets to add validation in a reactive way:

validations:
    Dict that is holding the validation results for any channel that has received updates. This channel can also be
    used for dirty field-check (all fields present in this Dict are dirty).

errors:
    Filtered version of 'validations' that only includes failing Validation's.
    
valid:
    Boolean channel that indicates if all fields in this Validator are valid.
    
valid(channel)
    Boolean channel that indicates the validation status of 'ch' 

invalid(channel)
    Boolean channel that indicates the validation status of 'ch' 

combinedErrors(channels*)
    Buffer[String] with the combined validation errors for the given 'channels' 


Form validation example:

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

# Validations

Validations are classes derived from the Validation base class and are used to validate some input. The input can be of
any type, but in the context of form validtion they normally validates a ReadChannel[String] channel. The Validation
base class has one abstract member (validate) that performs the actual validation of the provided input. There are a 
number of provided Vaidations in org.widok.validation.Validations.
 
## Error messages

A customized error message can be provided when initializing a Validation. This error message is interpolated using
variables that are defined in each Validation. For example:

```MinValidation(5, "Too few characters, minimum is: #{min}.. You wrote: #{value}")```
