package org.widok.validation

abstract class Validation[T](params: Map[String, Any] = Map.empty, message: String = "validation failed") {

  /**
   * Implement this method in subclasses to validate a value.
   *
   * @param value The value to be validated
   * @return Either a boolean indicating if validation is successful or an Optional string with a
   *         validation error message. If a boolean (false) is returned an error message will be constructed using the
   *         formatMessage method.
   */
  protected def validate(value: T): Either[Boolean, Option[String]]

  def validateValue(value: Any): Option[String] = {
    val typedValue: T = value.asInstanceOf[T]

    validate(typedValue) match {
      case Left(result) => if (result) None else Some(formatMessage(message, params, typedValue))
      case Right(validationMsg) => validationMsg
    }
  }

  def formatMessage(msg: String, params: Map[String, Any], value: T) = {
    val paramsWithValue = params + ("value" -> value)
    (msg /: paramsWithValue) { (res, entry) => res.replaceAll("#\\{%s\\}".format(entry._1), entry._2.toString.replace( """\""", """\\""")) }
  }
}

abstract class TextValidation(params: Map[String, Any] = Map.empty, message: String = "validation failed") extends Validation[String](params, message) {
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
   * @param min The minimum value
   * @param message Error message. Variables: #{value} the validated input, #{min} = the min value
   */
  case class MinValidation(min: Int, message: Option[String] = None)
    extends TextValidation(Map("min" -> min), message.getOrElse("Value must be at least #{min}")) {
    override def validate(value: String): Either[Boolean, Option[String]] = Left(if (value.forall(_.isDigit)) value.toInt >= min else false)
  }

  /**
   * Validates max values
   *
   * @param max The maximum value
   * @param message Error message. Variables: #{value} the validated input, #{max} tha max value
   */
  case class MaxValidation(max: Int, message: Option[String] = None)
    extends TextValidation(Map("max" -> max), message.getOrElse("Value must be at most #{max}")) {
    override def validate(value: String): Either[Boolean, Option[String]] = Left(if (value.forall(_.isDigit)) value.toInt <= max else false)
  }

  /**
   * Validate required values
   *
   * @param message Error message. Variables: #{value} the validated input
   */
  case class RequiredValidation(message: Option[String] = None)
    extends Validation[String](Map.empty, message.getOrElse("Required value")) {
    override def validate(value: String): Either[Boolean, Option[String]] = Left(!value.toString.isEmpty)
  }

  /**
   * Validates that values matches a regex pattern
   *
   * @param pattern Regex pattern
   * @param message Error message. Variables: #{value} the validated input, #{pattern}
   */
  case class PatternValidation(pattern: String, message: Option[String] = None)
    extends TextValidation(Map("pattern" -> pattern), message.getOrElse("Value must match pattern #{pattern}")) {
    override def validate(value: String): Either[Boolean, Option[String]] = Left(value.matches(pattern))
  }

  /**
   * Validates that a value is at least 'minLength' characters
   *
   * @param minLength Minimum length of value
   * @param message Error message. Variables: #{value} the validated input, #{minLength} = minimum length
   */
  case class MinLengthValidation(minLength: Int, message: Option[String] = None)
    extends TextValidation(Map("minLength" -> minLength), message.getOrElse("Value must have at least #{minLength} characters")) {
    override def validate(value: String): Either[Boolean, Option[String]] = Left(value.length >= minLength)
  }

  /**
   * Validates that a value is at most 'maxLength' characters
   *
   * @param maxLength Maximum length of value
   * @param message Error message. Variables: #{value} the validated input, #{maxLength} = maximum length
   */
  case class MaxLengthValidation(maxLength: Int, message: Option[String] = None)
    extends TextValidation(Map("maxLength" -> maxLength), message.getOrElse("Value must have at most #{maxLength} characters")) {
    override def validate(value: String): Either[Boolean, Option[String]] = Left(value.length <= maxLength)
  }

  /**
   * Validates that a value is a valid e-mail adress.
   *
   * @param message Error message. Variables: #{value} the validated input
   */
  case class EmailValidation(message: Option[String] = None)
    extends TextValidation(Map.empty, message.getOrElse("Must be a valid e-mail adress")) {
    override def validate(value: String): Either[Boolean, Option[String]] = Left(value.isEmpty || value.matches( """(?i)[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}"""))
  }

  /**
   * Validates that two tupled strings are equal.
   *
   * @param message Error message. Variables: #{value} the validated input
   */
  case class SameValidation(message: Option[String] = None)
    extends Validation[Tuple2[String, String]](Map.empty, message.getOrElse("Values must be equal")) {
    override def validate(value: Tuple2[String, String]): Either[Boolean, Option[String]] = Left(value._1 == value._2)
  }

  /**
   * Validates that a boolean value is true
   *
   * @param message Variables: #{value} the validated input
   */
  case class TrueValidation(message: Option[String] = None) extends Validation[Boolean](message = message.getOrElse("Must be true")) {
    override protected def validate(value: Boolean): Either[Boolean, Option[String]] = Left(value)
  }

  /**
   * Validates that a boolean value is false
   *
   * @param message Variables: #{value} the validated input
   */
  case class FalseValidation(message: Option[String] = None) extends Validation[Boolean](message = message.getOrElse("Must be false")) {
    override protected def validate(value: Boolean): Either[Boolean, Option[String]] = Left(!value)
  }
}