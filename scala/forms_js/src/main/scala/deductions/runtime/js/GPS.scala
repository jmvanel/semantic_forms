package deductions.runtime.js

import scala.scalajs.js

import org.scalajs.dom
// for for loop with NodeList:
import org.scalajs.dom.ext.PimpedNodeList
import org.scalajs.dom.html.Input
import org.scalajs.dom.raw.Node
import org.scalajs.dom.raw.Position
import org.scalajs.dom.raw.NodeList

/** callback for Geo HTML5 function watchPosition()
 *  */
trait GPS {

  /** callback for Geo HTML5 function watchPosition() that fills coordinates
 * in relevant <input> elements;
 * so everytime a new GPS Position is received, the form is updated;
 * after saving form, the server will populate RDF triples:
 *   ?S geo:long ?LONG .
 *   ?S geo:lat  ?LAT .
 */
  def fillCoords(p: Position) = {
    val coords = p.coords
    val longitude = coords.longitude
    val latitude = coords.latitude
    val altitude = coords.altitude
    println(s"Got from GPS: latitude=${latitude}, longitude=${longitude}, altitude=${altitude}")
    println(s"(La précision est de ${coords.accuracy} mètres.)")

    // TODO Handling abnormal location results, see http://www.andygup.net/how-accurate-is-html5-geolocation-really-part-2-mobile-web/

    val geoCoordinatesFields = GeoCoordinatesFields.pageNeedsGeoCoordinates()
    for (
      longitudeInput <- geoCoordinatesFields.matchesLongitudeInput;
      latitudeInput <- geoCoordinatesFields.matchesLatitudeInput
    ) {
      fillOneCoordinate(longitudeInput, longitude.toString())
      fillOneCoordinate(latitudeInput, latitude.toString())
    }
    // altitude Input is facultative in form
    for ( altitudeInput <- geoCoordinatesFields.matchesAltitudeInput ) {
      fillOneCoordinate(altitudeInput, altitude.toString())
    }
  }

  /** fill One Coordinate in <input> element */
  private def fillOneCoordinate(node: Node, coord: String): Any = {
    // println( s"fillOneCoordinate( $l: Node, $coord)" )
    node match {
      case input: Input =>
        input.value = coord
        print(s"fillGeoCoordinates: value ${input.value}, ")
      case el => print(s"fillGeoCoordinates: $el unexpected, ")
    }
  }
}

case class GeoCoordinatesFields(
    needsUpdate: Boolean,
    matchesLongitudeInput: NodeList,
    matchesLatitudeInput: NodeList,
    matchesAltitudeInput: NodeList
    )

object GeoCoordinatesFields {
  	// TODO later depend on module utils
  val geoRDFPrefix = "http://www.w3.org/2003/01/geo/wgs84_pos#"

  def pageNeedsGeoCoordinates(): GeoCoordinatesFields = {
    val matchesLongitudeInput = dom.document.querySelectorAll(
      s"input[data-rdf-property='${geoRDFPrefix}long']")
    val matchesLatitudeInput = dom.document.querySelectorAll(
      s"input[data-rdf-property='${geoRDFPrefix}lat']")
    val matchesAltitudeInput = dom.document.querySelectorAll(
      s"input[data-rdf-property='${geoRDFPrefix}alt']")

    print(s"matchesLongitudeInput $matchesLongitudeInput, length ${matchesLongitudeInput.length}")
    val inputIsEmptyLongitude = inputIsEmpty(matchesLongitudeInput)
    val longitudeFieldExists = matchesLongitudeInput.length > 0
    val needs =
      ( matchesLatitudeInput.length > 0 ||
        longitudeFieldExists ) &&
        inputIsEmptyLongitude
    println("needs GPS input: " + needs)
    if( longitudeFieldExists && ! inputIsEmptyLongitude )
      println("Longitude is already filled: '" + matchesLongitudeInput + "'")
    GeoCoordinatesFields(needs, matchesLongitudeInput, matchesLatitudeInput, matchesAltitudeInput)
  }

  /** input field exists and Is Empty */
  private def inputIsEmpty(nodeList: NodeList): Boolean = {
    print("inputIsEmpty(): nodeList.length: " + nodeList.length)
    nodeList . map {
      case input: Input =>
        if( input.value.size > 0 ) print("inputIsEmpty(): input.value: '" +  input.value.toString + "'")
        input.value == ""
      case _ => true
    } . headOption . getOrElse(true)
  }

  private def print(m: String) = dom.window.console.info(m)
}
