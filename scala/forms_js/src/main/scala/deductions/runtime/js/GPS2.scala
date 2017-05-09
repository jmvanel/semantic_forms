package deductions.runtime.js

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.annotation.JSExport

import org.scalajs.dom
import org.scalajs.dom.window
import org.scalajs.dom.document
import org.scalajs.dom.raw.Geolocation
import org.scalajs.dom.raw.Position
import org.scalajs.dom.raw.PositionError
import org.scalajs.dom.raw.Node
import org.scalajs.dom.html.Input
// for for loop with NodeList:
import org.scalajs.dom.ext._

@JSExportTopLevel("GPS2")
object GPS2 {

  // TODO later depend on utils
  val geoRDFPrefix = "http://www.w3.org/2003/01/geo/wgs84_pos#"

  /**
   * listen To Submit Event and Fill Geo Coordinates
   *  TODO not sure if the updated <input> content goes to the server ...
   */
  @JSExport
  def listenToSubmitEventFillGeoCoordinates(): Unit = {
     window.addEventListener("load", (e: dom.Event) => fillGeoCoordinates)
  }

  private def listenToSubmitEventFillGeoCoordinates_OLD(): Unit = {
    window.console.log("""listenToSubmitEventFillGeoCoordinates (Scala.js) """)
    val buttons = getSaveButtons
    window.console.log(s"""GPS2 Buttons: ${buttons.size} $buttons""")
    for (button <- buttons) {
      //    getSaveButtons.addEventListener("submit",
      button.addEventListener("onclick",
        (e: dom.Event) => {
          window.console.log("""GPS2 Event("submit") """)
          fillGeoCoordinates
        },
        false); // Modern browsers
      window.console.log(s"GPS2 Button: Event added !")
      window.console.log(button)
    }
  }

  private def getSaveButtons = {
    // QUESTION: why is the second criterion value=SAUVER not used ? 
    //    dom.document.querySelectorAll("[type=submit], [value=SAUVER]")
    document.querySelectorAll("[value=SAUVER]")
  }

  /**
   * fill longitude & latitude from HTML5 API into relevant geo:long & geo:lat triples
   */
  private def fillGeoCoordinates {
    val matchesLongitude = dom.document.querySelectorAll(
      s"input[data-uri-property='${geoRDFPrefix}long']")
    val matchesLatitude = dom.document.querySelectorAll(
      s"input[data-uri-property='${geoRDFPrefix}lat']")

    window.console.log(s"fillGeoCoordinates: before GPS.geoLocation: matchesLongitude $matchesLongitude")
    val coordsOption = GPS.geoLocation()
    window.console.log(s"fillGeoCoordinates: after GPS.geoLocation : $coordsOption")
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