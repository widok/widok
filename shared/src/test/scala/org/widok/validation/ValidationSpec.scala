package org.widok.validation

import minitest._

object FieldValidationSpec extends SimpleTestSuite {

  class TestFieldValidation(result: Boolean) extends FieldValidation[String]() {
    override protected def validate(value: String): Either[Boolean, Option[String]] = Left(result)
  }

  test("should default to 'validation failed' error message") {
    assertEquals(new TestFieldValidation(false).validateValue("value"), Some("validation failed"))
  }

  test("should return None when validation pass") {
    assertEquals(new TestFieldValidation(true).validateValue("value"), None)
  }

  class CustomMessageValidator(params: Map[String, Any], message: String) extends FieldValidation[String](params, message) {
    override protected def validate(value: String): Either[Boolean, Option[String]] = Left(false)
  }

  test("should interpolate error message") {
    val v = new CustomMessageValidator(Map("paramA" -> "valueA", "paramB" -> "valueB"), "custom message, p1: #{paramA}, p2: #{paramB}")

    assertEquals(v.validateValue("value"), Some("custom message, p1: valueA, p2: valueB"))
  }

  test("should be able to affect validation message") {

    case class ImpossibleValidation() extends FieldValidation[String]() {
      override protected def validate(value: String): Either[Boolean, Option[String]] = if (value.isEmpty) Left(false) else Right(Some("Must not have length"))
    }

    assertEquals(ImpossibleValidation().validateValue(""), Some("validation failed"))
    assertEquals(ImpossibleValidation().validateValue("some text"), Some("Must not have length"))

  }

}

object ValidationsSpec extends SimpleTestSuite {
  import org.widok.validation.Validations._

  test("TextFieldValidation childs should throw exception when wrong type input") {
    class TestValidation() extends TextFieldValidation() {
      override protected def validate(value: String): Either[Boolean, Option[String]] = Left(true)
    }
    intercept[IllegalArgumentException] {
      new TestValidation().validateValue(100)
    }
  }

  test("MinValidation should validate min values") {
    assert(MinValidation(50).validateValue("").isEmpty)
    assert(MinValidation(50).validateValue("51").isEmpty)
    assert(MinValidation(50).validateValue("50").isEmpty)
    assert(MinValidation(50).validateValue("49").isDefined)
    assert(MinValidation(50).validateValue("some text").isDefined)
  }

  test("MaxValidation should validateValue() max values") {
    assert(MaxValidation(50).validateValue("").isEmpty)
    assert(MaxValidation(50).validateValue("49").isEmpty)
    assert(MaxValidation(50).validateValue("50").isEmpty)
    assert(MaxValidation(50).validateValue("51").isDefined)
    assert(MaxValidation(50).validateValue("some text").isDefined)
  }

  test("RequiredValidation should validate required values") {
    assert(RequiredValidation().validateValue("").isDefined)
    assert(RequiredValidation().validateValue("some value").isEmpty)
  }

  test("PatternValidation should validate by pattern") {
    assert(PatternValidation(".{2}").validateValue("").isEmpty)
    assert(PatternValidation(".{2}").validateValue("error").isDefined)
    assert(PatternValidation(".{2}").validateValue("ok").isEmpty)
  }

  test("MinLengthValidation should validate min length") {
    assert(MinLengthValidation(5).validateValue("").isEmpty)
    assert(MinLengthValidation(5).validateValue("1234").isDefined)
    assert(MinLengthValidation(5).validateValue("12345").isEmpty)
    assert(MinLengthValidation(5).validateValue("123456").isEmpty)
  }

  test("MaxLengthValidation should validate max length") {
    assert(MaxLengthValidation(5).validateValue("").isEmpty)
    assert(MaxLengthValidation(5).validateValue("123456").isDefined)
    assert(MaxLengthValidation(5).validateValue("12345").isEmpty)
    assert(MaxLengthValidation(5).validateValue("1234").isEmpty)
  }

  test("EmailValidation should validate email adresses") {
    assert(EmailValidation().validateValue("").isEmpty)
    assert(EmailValidation().validateValue("some text").isDefined)
    assert(EmailValidation().validateValue("name@mail.com").isEmpty)
    assert(EmailValidation().validateValue("name@mail").isDefined)
    assert(EmailValidation().validateValue("name@mail.longish").isDefined)
  }

  test("SameValidation should validate that two values are equal") {
    assert(SameValidation().validateValue("ok", "ok").isEmpty)
    assert(SameValidation().validateValue("", "").isEmpty)
    assert(SameValidation().validateValue("ok", "").isDefined)
    assert(SameValidation().validateValue("ok", "nok").isDefined)
  }

  test("TrueValidation should validate that a boolean value is true") {
    assert(TrueValidation().validateValue(true).isEmpty)
    assert(TrueValidation().validateValue(false).isDefined)
  }

  test("FalseValidation should validate that a boolean value is false") {
    assert(FalseValidation().validateValue(false).isEmpty)
    assert(FalseValidation().validateValue(true).isDefined)
  }

}
