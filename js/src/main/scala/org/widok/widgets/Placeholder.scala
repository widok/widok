package org.widok.widgets

import org.scalajs.dom
import org.widok.bindings.HTML

/**
 * Based upon angular-placeholders (Josh David Miller)
 * @see https://github.com/joshdmiller/angular-placeholders
 **/
object Placeholder {
  case class Size(width: Int, height: Int) {
    override def toString = s"${width}x$height"
  }

  def calculateTextSize(textSize: Int, size: Size): Int = {
    val maxDimension = Math.max(size.height, size.width)
    val maxFactor = Math.round(maxDimension.toDouble / 16)

    Math.max(textSize, maxFactor).toInt
  }

  def draw(text: String,
           size: Size,
           textSize: Int,
           fillColour: String,
           textColour: String
          ): String =
  {
    val canvas = dom.document.createElement("canvas").asInstanceOf[dom.HTMLCanvasElement]
    val context = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
    val textSizePt = calculateTextSize(textSize, size)

    canvas.width = size.width
    canvas.height = size.height
    context.fillStyle = fillColour
    context.fillRect(0, 0, size.width, size.height)
    context.fillStyle = textColour
    context.textAlign = "center"
    context.textBaseline = "middle"
    context.font = s"bold ${textSizePt}pt sans-serif"

    if (context.measureText(text).width / size.width > 1) {
      val textSizePt = textSize / (context.measureText(text).width / size.width)
      context.font = s"bold ${textSizePt}pt sans-serif"
    }

    context.fillText(text, size.width / 2, size.height / 2)
    canvas.toDataURL("image/png")
  }

  def apply(text: String,
            size: Size,
            textSize: Int = 10,
            fillColour: String = "#EEEEEE",
            textColour: String = "#AAAAAA"
           ): HTML.Image =
    HTML.Image(draw(text, size, textSize, fillColour, textColour))
      .attribute("title", size.toString)
      .attribute("alt", size.toString)
}