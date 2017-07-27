package deductions.runtime.js

import scala.scalajs.js

import org.scalajs.dom
// for for loop with NodeList:
import org.scalajs.dom.ext.PimpedNodeList
import org.scalajs.dom.html.Input
import org.scalajs.dom.raw.Node
import org.scalajs.dom.raw.Position
import org.scalajs.dom.raw.NodeList

/** callback for Geo HTML5 function that populates RDF triples geo:long */
trait GPS {

  /** callback for Geo HTML5 function */
  def fillCoords(p: Position) = {

    val longitude = p.coords.longitude
    val latitude = p.coords.latitude
    println(s"latitude=${latitude}")
    println(s"longitude=${longitude}")

//    val matchesLongitudeInput = dom.document.querySelectorAll(
//      s"input[data-rdf-property='${geoRDFPrefix}long']")
//    val matchesLatitudeInput = dom.document.querySelectorAll(
//      s"input[data-rdf-property='${geoRDFPrefix}lat']")

    val geoCoordinatesFields = GeoCoordinatesFields.pageNeedsGeoCoordinates()
    
    for (
      long <- geoCoordinatesFields.matchesLongitudeInput;
      lat <- geoCoordinatesFields.matchesLatitudeInput
    ) {
      fillOneCoordinate(long, longitude.toString())
      fillOneCoordinate(lat, latitude.toString())
    }
  }

  private def fillOneCoordinate(l: Node, coord: String) = {
//    println( s"fillOneCoordinate( $l: Node, $coord)" )
    l match {
    case input: Input => input.value = coord
    dom.window.console.info(s"fillGeoCoordinates: value ${input.value}")
    case el           => dom.window.console.info(s"fillGeoCoordinates: $el unexpected")
  }
  }
}

case class GeoCoordinatesFields(needs: Boolean, matchesLongitudeInput: NodeList,
    matchesLatitudeInput: NodeList)

object GeoCoordinatesFields {
  	// TODO later depend on module utils
  val geoRDFPrefix = "http://www.w3.org/2003/01/geo/wgs84_pos#"

  def pageNeedsGeoCoordinates(): GeoCoordinatesFields = {
    val matchesLongitudeInput = dom.document.querySelectorAll(
      s"input[data-rdf-property='${geoRDFPrefix}long']")
    val matchesLatitudeInput = dom.document.querySelectorAll(
      s"input[data-rdf-property='${geoRDFPrefix}lat']")

    dom.window.console.info(s"matchesLongitudeInput $matchesLongitudeInput, length ${matchesLongitudeInput.length}")
    // dom.window.console.info(s"matchesLatitudeInput $matchesLatitudeInput" )
    val needs =
      matchesLatitudeInput.length > 0 ||
        matchesLongitudeInput.length > 0
    GeoCoordinatesFields(needs, matchesLongitudeInput, matchesLatitudeInput)
  }
}