package deductions.runtime.js

import scala.scalajs.js

import org.scalajs.dom
// for for loop with NodeList:
import org.scalajs.dom.ext.PimpedNodeList
import org.scalajs.dom.html.Input
import org.scalajs.dom.raw.Node
import org.scalajs.dom.raw.Position

trait GPS {

	// TODO later depend on utils
  val geoRDFPrefix = "http://www.w3.org/2003/01/geo/wgs84_pos#"


  /** callback for Geo HTML5 function */
  def fillCoords(p: Position) = {
    val longitude = p.coords.longitude
    val latitude = p.coords.latitude
    println(s"latitude=${latitude}")
    println(s"longitude=${longitude}")

    val matchesLongitudeInput = dom.document.querySelectorAll(
      s"input[data-uri-property='${geoRDFPrefix}long']")
    val matchesLatitudeInput = dom.document.querySelectorAll(
      s"input[data-uri-property='${geoRDFPrefix}lat']")

    dom.window.console.info(s"matchesLongitudeInput $matchesLongitudeInput" )
    dom.window.console.info(s"matchesLatitudeInput $matchesLatitudeInput" )

    for (
      long <- matchesLongitudeInput;
      lat <- matchesLatitudeInput
    ) {
      fillOneCoordinate(long, longitude.toString())
      fillOneCoordinate(lat, latitude.toString())
    }
  }

  private def fillOneCoordinate(l: Node, coord: String) = l match {
    case input: Input => input.value = coord
    case el           => dom.window.console.info(s"fillGeoCoordinates: $el unexpected")
  }
}