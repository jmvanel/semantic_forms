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
import org.scalajs.dom.raw.PositionOptions
// for for loop with NodeList:
import org.scalajs.dom.ext._
import scala.scalajs.js.Dynamic.{literal => json}
import org.scalajs.dom.raw.HTMLElement
import org.w3c.dom.events.Event

@JSExportTopLevel("GPS2")
object GPS2 extends GPS {

    val window = dom.document.defaultView
    val nav = window.navigator
    val geo: Geolocation = nav.geolocation
    val gpsParameters = json(
		   enableHighAccuracy=true,
		   maximumAge=20000,
		   timeout=30000 )
    var watchID: Int = -1

  /** start watching Position from GPS,
   * to fill Geo Coordinates from GPS in form
   * 
   * Called in page tail in
   * forms/src/main/resources/deductions/runtime/html/tail.html
   */
  @JSExport
  def listenToSubmitEventFillGeoCoordinates(): Unit = {
    if (GeoCoordinatesFields.pageNeedsGeoCoordinates().needsUpdate)
      // window.addEventListener("load", (e: dom.Event) =>
    {
      fillPositionsOnce(); println("Called fillPositionsOnce")
      fillGeoCoordinates
      // stop using GPS when user saves form
      val buttons = dom.document.getElementsByName("save")
      for (button <- buttons) {
        button.addEventListener(
          "click",
          (ev: Event) => {
            dom.console.log("User saves form: stop GPS")
            clearWatch()
          })
      }
    }
  }

  @JSExport
  /** When user empties longitude, start GPS filling */
  def listenToEmptyInputLongitude(): Unit = {
    val matchesLongitudeInput = GeoCoordinatesFields.matchesGeoCoordinatesInput("long")
    for (node <- matchesLongitudeInput) {
      node.addEventListener(
        "input",
        (ev: dom.Event) => {
          node match {
            case input: Input if (input.value == "") =>
              fillGeoCoordinates
              print(s"Empty longitude => fillGeoCoordinates started")
          }
        })
    }
  }
  /**
   * fill continuously longitude & latitude from HTML5 GPS API into relevant geo:long & geo:lat triples ,
   * See https://developer.mozilla.org/fr/docs/Using_geolocation
   */
  private def fillGeoCoordinates {
    watchID = geo.watchPosition( fillCoords _, onError _,
     gpsParameters.asInstanceOf[PositionOptions] )
    println("Callback fillCoords() set by watchPosition")
  }

  @JSExport
  def clearWatch(): Unit = {
    geo.clearWatch(watchID);
    watchID = -1
  }

  import scala.scalajs.js.Dynamic.global

  def onError(p: PositionError) = {
    val message = "geoLocation: Error: " + p.message + " - " + p
    global.console.log(message)
    val appMessagesZone = dom. document.getElementById("appMessages")
    appMessagesZone.innerHTML = message
    appMessagesZone match {
      case elem: HTMLElement => elem.style = // "Color: red"
      "Background-Color: red"
      case _ =>
    }
    import scala.scalajs.js.timers._
    setTimeout( 40000 ) { appMessagesZone.innerHTML = "" }
  }

  @JSExport
  def fillPositionsOnce() = {
    geo.getCurrentPosition( fillCoords _, onError _ ,
      gpsParameters.asInstanceOf[PositionOptions] )
  }

}
