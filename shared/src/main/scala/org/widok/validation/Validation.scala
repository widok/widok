package org.widok.validation

abstract class FieldValidation[T](val params: Map[String, Any] = Map.empty, message: String = "validation failed") {

  protected def validate(value: T): Boolean

  def validateValue(value: Any): Option[String] = {
    val typedValue: T = value.asInstanceOf[T]
    if (validate(typedValue)) None else Some(formatMessage(typedValue))
  }

  def formatMessage(value: T) = {
    val paramsWithValue = params + ("value" -> value)
    (message /: paramsWithValue) { (res, entry) => res.replaceAll("#\\{%s\\}".format(entry._1), entry._2.toString.replace( """\""", """\\""")) }
  }
}

abstract class TextFieldValidation(params: Map[String, Any] = Map.empty, message: String = "validation failed") extends FieldValidation[String](params, message) {
  override def validateValue(value: Any): Option[String] = {
    if (!value.isInstanceOf[String]) throw new IllegalArgumentException("Expected a string value")
    val typedValue = value.asInstanceOf[String]
    if (typedValue.isEmpty) None else super.validateValue(typedValue)
  }
}

object Validations {

  /**
   * Validates min values
   *
   * @param min The minimum value for the field
   * @param message Error message. Variables: #{value} the validated input, #{min} = the min value
   */
  case class MinValidation(min: Int, message: Option[String] = None)
    extends TextFieldValidation(Map("min" -> min), message.getOrElse("Value must be at least #{min}")) {
    override def validate(value: String): Boolean = if (value.forall(_.isDigit)) value.toInt >= min else false
  }

  /**
   * Validates max values
   *
   * @param max The maximum value for the field
   * @param message Error message. Variables: #{value} the validated input, #{max} tha max value
   */
  case class MaxValidation(max: Int, message: Option[String] = None)
    extends TextFieldValidation(Map("max" -> max), message.getOrElse("Value must be at most #{min}")) {
    override def validate(value: String): Boolean = if (value.forall(_.isDigit)) value.toInt <= max else false
  }

  /**
   * Validate required values
   *
   * @param message Error message. Variables: #{value} the validated input
   */
  case class RequiredValidation(message: Option[String] = None)
    extends FieldValidation[String](Map.empty, message.getOrElse("Required value")) {
    override def validate(value: String): Boolean = !value.toString.isEmpty
  }

  /**
   * Validates that values matches a regex pattern
   *
   * @param pattern Regex pattern
   * @param message Error message. Variables: #{value} the validated input, #{pattern}
   */
  case class PatternValidation(pattern: String, message: Option[String] = None)
    extends TextFieldValidation(Map("pattern" -> pattern), message.getOrElse("Value must match pattern #{pattern}")) {
    override def validate(value: String): Boolean = {
      value.matches(pattern)
    }
  }

  /**
   * Validates that a value is at least 'minLength' characters
   *
   * @param minLength Minimum length of value
   * @param message Error message. Variables: #{value} the validated input, #{minLength} = minimum length
   */
  case class MinLengthValidation(minLength: Int, message: Option[String] = None)
    extends TextFieldValidation(Map("minLength" -> minLength), message.getOrElse("Value must have at least #{minLength} characters")) {
    override def validate(value: String): Boolean = {
      value.length >= minLength
    }
  }

  /**
   * Validates that a value is at most 'maxLength' characters
   *
   * @param maxLength Maximum length of value
   * @param message Error message. Variables: #{value} the validated input, #{maxLength} = maximum length
   */
  case class MaxLengthValidation(maxLength: Int, message: Option[String] = None)
    extends TextFieldValidation(Map("maxLength" -> maxLength), message.getOrElse("Value must have at most #{maxLength} characters")) {
    override def validate(value: String): Boolean = {
      value.length <= maxLength
    }
  }

  /**
   * Validates that a value is a valid email adress.
   *
   * @param message Error message. Variables: #{value} the validated input
   */
  case class EmailValidation(message: Option[String] = None)
    extends TextFieldValidation(Map.empty, message.getOrElse("Must be a valid email-adress")) {
    override def validate(value: String): Boolean = {
      value.isEmpty || value.matches( """(?i)[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}""")
    }
  }

  /**
   * Validates that two tupled strings are equal.
   *
   * @param message Error message. Variables: #{value} the validated input,
   */
  case class SameValidation(message: Option[String] = Some("Fields must be equal"))
    extends FieldValidation[Tuple2[String, String]](Map.empty, message.getOrElse("Must be a valid email-adress")) {
    override def validate(value: Tuple2[String, String]): Boolean = {
      value._1 == value._2
    }
  }
}