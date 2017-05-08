package deductions.runtime.js

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

import org.scalajs.dom
import org.scalajs.dom.raw.Geolocation
import org.scalajs.dom.raw.Position
import org.scalajs.dom.raw.PositionError
import org.scalajs.dom.raw.Node
import org.scalajs.dom.html.Input
// for for loop with NodeList:
import org.scalajs.dom.ext._

@JSExportTopLevel("GPS")
object GPS2 {

  // TODO later depend on utils
  val geoPrefix = "http://www.w3.org/2003/01/geo/wgs84_pos#"

  /** TODO not sure if the updated <input> content goes to the server ... */
  def listenToSubmitEventFillGeoCoordinates = {
    getSaveButtons.addEventListener("submit",
      (e: dom.Event) => {
        println("""EventListener("submit") """)
        fillGeoCoordinates },
      false); // Modern browsers
  }

  private def getSaveButtons = {
    dom.document.querySelector("[type=submit]")
  }

  /**
   * fill longitude & latitude from HTML5 API into relevant geo:long & geo:lat triples
   */
  private def fillGeoCoordinates {
    val matchesLongitude = dom.document.querySelectorAll(
      s"input[data-uri-property=${geoPrefix}long]")
    val matchesLatitude = dom.document.querySelectorAll(
      s"input[data-uri-property=${geoPrefix}lat]")

    val coordsOption = GPS.geoLocation()

    for (
      coords <- coordsOption;
      long <- matchesLongitude;
      lat <- matchesLatitude
    ) {
      fillOneCoordinate(long, coords._1.toString())
      fillOneCoordinate(lat, coords._2.toString())
    }
  }

  private def fillOneCoordinate(l: Node, coord: String) = l match {
    case input: Input => input.value = coord
    case el           => dom.window.console.info(s"fillGeoCoordinates: $el unexpected")
  }

}