package org.widok.validation

import org.widok.{Dict, ReadStateChannel}

case class Validator(validationSources: Tuple2[ReadStateChannel[_], Seq[FieldValidation[_]]]*) {

  val validations = Dict[ReadStateChannel[_], Seq[String]]()

  val valid = validations.forall(_.isEmpty).cache(true)
  val errors = validations.filter(_.nonEmpty).buffer

  validationSources.foreach {
    case (ch, fv) =>
      // don't validate initial values (tail) and only validate when the value is actually updated (distinct)
      ch.distinct.tail.attach(input => validateValue(ch, fv, input))
  }

  private def validateValue(ch: ReadStateChannel[_], fv: Seq[FieldValidation[_]], input: Any) = {
    validations.insertOrUpdate(ch, fv.flatMap(_.validateValue(input)))
  }

  def validate() = {
    validationSources.filterNot(s => validations.keys$.contains(s._1)).foreach {
      case (ch, fv) => validateValue(ch, fv, ch.get)
    }
  }
}