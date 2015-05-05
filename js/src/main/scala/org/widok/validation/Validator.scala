package org.widok.validation

import org.widok.{Dict, ReadStateChannel}

case class Validator(validationSources: Tuple2[ReadStateChannel[_], Seq[FieldValidation[_]]]*) {

  val validations = Dict[ReadStateChannel[_], Seq[String]]()

  val valid = validations.forall(_.isEmpty).cache(true)
  val errors = validations.filter(_.nonEmpty).buffer

  validationSources.foreach {
    case (channel, fieldValidators) =>
      // don't validate initial values (tail) and only validate when the value is actually updated (distinct)
      channel.distinct.tail.attach(input => validateValue(channel, fieldValidators, input))
  }

  private def validateValue(channel: ReadStateChannel[_], fieldValidators: Seq[FieldValidation[_]], input: Any) = {
    validations.insertOrUpdate(channel, fieldValidators.flatMap(_.validateValue(input)))
  }

  def validate() : Boolean = {
    validationSources.filterNot(s => validations.keys$.contains(s._1)).foreach {
      case (channel, fieldValidators) => validateValue(channel, fieldValidators, channel.get)
    }
    valid.get
  }
}