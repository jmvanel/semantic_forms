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
    println(s"Got from GPS: latitude=${latitude} , longitude=${longitude}")

    val geoCoordinatesFields = GeoCoordinatesFields.pageNeedsGeoCoordinates()
    if( geoCoordinatesFields . needsUpdate )
    for (
      longitudeInput <- geoCoordinatesFields.matchesLongitudeInput;
      latitudeInput <- geoCoordinatesFields.matchesLatitudeInput
    ) {
      fillOneCoordinate(longitudeInput, longitude.toString())
      fillOneCoordinate(latitudeInput, latitude.toString())
    }
    
  }

  private def fillOneCoordinate(node: Node, coord: String): Any = {
    // println( s"fillOneCoordinate( $l: Node, $coord)" )
    node match {
      case input: Input =>
        input.value = coord
        dom.window.console.info(s"fillGeoCoordinates: value ${input.value}")
      case el => dom.window.console.info(s"fillGeoCoordinates: $el unexpected")
    }
  }
}

case class GeoCoordinatesFields(needsUpdate: Boolean,
    matchesLongitudeInput: NodeList,
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
    val inputIsEmptyLongitude = inputIsEmpty(matchesLongitudeInput)
    val needs =
      ( matchesLatitudeInput.length > 0 ||
        matchesLongitudeInput.length > 0 ) &&
        inputIsEmptyLongitude
    if( ! inputIsEmptyLongitude )
      println("Longitude is already filled => not update from GPS.")
    GeoCoordinatesFields(needs, matchesLongitudeInput, matchesLatitudeInput)
  }

  private def inputIsEmpty(nodeList: NodeList): Boolean = {
    nodeList . map {
      case input: Input => input.value == ""
      case _ => true
    } . headOption . getOrElse(false)
  }
}