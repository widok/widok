package org.widok.validation

import org.widok.{Buffer, Dict, ReadChannel}

case class Validator(validationSources: Tuple2[ReadChannel[_], Seq[Validation[_]]]*) {

  val validations = Dict[ReadChannel[_], Seq[String]]()

  val valid = validations.forall(_.isEmpty).cache(true)

  val errors = validations.filter(_.nonEmpty).buffer

  def valid(ch: ReadChannel[_]) = errors.value(ch).isEmpty

  def invalid(ch: ReadChannel[_]) = errors.value(ch).isDefined

  // combines the validation results from several channels
  def combinedErrors(channels: ReadChannel[_]*): Buffer[String] = {
    val result = Buffer[String]()
    var messagesByChannel = channels.map(key => key -> Seq[String]()).toMap[ReadChannel[_], Seq[String]]

    channels.map(errors.value(_).values).map { channel =>
      channel.attach { optCh =>
        messagesByChannel += (channel -> optCh.getOrElse(Seq.empty))
        result.set(messagesByChannel.values.flatten.toSet.toSeq) // set all unique errors
      }
    }
    result
  }

  validationSources.foreach {
    case (ch, fv) =>
      // don't validate initial values (tail) and only validate when the value is actually updated (distinct)
      ch.distinct.tail.attach(input => validateValue(ch, fv, input))
  }

  private def validateValue(ch: ReadChannel[_], fv: Seq[Validation[_]], input: Any) {
    validations.insertOrUpdate(ch, fv.flatMap(_.validateValue(input)))
  }

  def validate(): Boolean = {
    // flush and validate and values that haven't been updated (aren't dirty) yet
    validationSources.filterNot(s => validations.keys$.contains(s._1)).foreach {
      case (ch, fv) => ch.flush(validateValue(ch, fv, _))
    }
    valid.get
  }
}