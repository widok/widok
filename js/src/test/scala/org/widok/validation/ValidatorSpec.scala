package org.widok.validation

import minitest._
import org.widok.validation.Validations.{SameValidation, RequiredValidation, MinLengthValidation, PatternValidation}
import org.widok.{StateChannel, Var}

case class Fixture(ch1: StateChannel[String], ch2: StateChannel[String], validator: Validator)

object ValidatorSpec extends SimpleTestSuite {

  class DefaultFixture {
    val ch1 = Var("valid")
    val ch2 = Var("invalid")
    val combined = ch1.combine(ch2).cache("", "")

    val patternValidation: PatternValidation = PatternValidation(".{3,6}")

    val validator = Validator(
      ch1 -> Seq(patternValidation, RequiredValidation()),
      ch2 -> Seq(patternValidation),
      combined -> Seq(SameValidation())
    )
  }

  test("should not validate initial values") {
    new DefaultFixture {
      assert(validator.valid.get)
    }
  }

  test("should validate on input") {
    new DefaultFixture {
      ch1 := "invalid"
      assertEquals(validator.errors.value$(ch1), Seq("Value must match pattern .{3,6}"))
    }
  }

  test("should clear validation errors when corrected") {
    new DefaultFixture {
      ch1 := "invalid"
      assertEquals(validator.errors.value$(ch1), Seq("Value must match pattern .{3,6}"))
      ch1 := "valid"
      assertEquals(validator.errors.keys$.contains(ch1), false)
    }
  }

  test("should validate all fields when requested") {
    new DefaultFixture {
      validator.validate()
      assertEquals(validator.errors.keys$.contains(ch1), false)
      assertEquals(validator.errors.value$(ch2), Seq("Value must match pattern .{3,6}"))
    }
  }

  test("should report multiple validation errors for a single field") {
    val ch = Var("aaa")
    val validator = Validator(ch -> Seq(MinLengthValidation(5), PatternValidation(".{5, 6}")))
    validator.validate()
    assertEquals(validator.errors.value$(ch), Seq("Value must have at least 5 characters", "Value must match pattern .{5, 6}"))
  }

  test("should return validation state") {
    new DefaultFixture {
      val invalid1 = validator.invalid(ch1).cache(true)
      val valid1 = validator.valid(ch1).cache(true)
      val invalid2 = validator.invalid(ch2).cache(true)
      val valid2 = validator.valid(ch2).cache(true)
      validator.validate()
      assert(!invalid1.get)
      assert(invalid2.get)
      assert(valid1.get)
      assert(!valid2.get)
    }
  }

  test("should return combined errors") {
    new DefaultFixture {
      val c = validator.combinedErrors(ch1, ch2)
      ch1 := ""
      ch2 := "a"
      validator.validate()
      assertEquals(c.get, Seq("Required value", "Value must match pattern .{3,6}"))
    }
  }
}

