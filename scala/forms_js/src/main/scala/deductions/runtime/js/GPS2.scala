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

@JSExportTopLevel("GPS2")
object GPS2 extends GPS {

  /** start watching Position from GPS,
   * to fill Geo Coordinates from GPS in form
   * 
   * Called in <head> in
   * forms/src/main/resources/deductions/runtime/html/head.html
   */
  @JSExport
  def listenToSubmitEventFillGeoCoordinates(): Unit = {
    if( GeoCoordinatesFields.pageNeedsGeoCoordinates() . needsUpdate )
      window.addEventListener("load", (e: dom.Event) => fillGeoCoordinates)
  }

  /**
   * fill longitude & latitude from HTML5 GPS API into relevant geo:long & geo:lat triples ,
   * See https://developer.mozilla.org/fr/docs/Using_geolocation
   */
  private def fillGeoCoordinates {
	  val window = dom.document.defaultView
    val nav = window.navigator
    val geo: Geolocation = nav.geolocation

   /* obtain longitude & latitude from HTML5 GPS API,
    * and then call fillCoords();
    * cf http://stackoverflow.com/questions/40483880/geolocation-in-scala-js */
    import scala.scalajs.js.Dynamic.{literal => json}
    val gpsParameters = json(
//     new org.scalajs.dom.raw.PositionOptions {
		   enableHighAccuracy=true,
		   maximumAge=20000,
		   timeout=15000 )
    geo.watchPosition( fillCoords _, onError _ , gpsParameters.asInstanceOf[PositionOptions] )
    print("Callback fillCoords() set by watchPosition")
  }

  import scala.scalajs.js.Dynamic.global

  def onError(p: PositionError) = {
    val message = "geoLocation: Error: " + p.message + " - " + p
    global.console.log(message)
    val appMessagesZone = dom. document.getElementById("appMessages")
    appMessagesZone.innerHTML = message
    import scala.scalajs.js.timers._
    setTimeout( 40000 ) { appMessagesZone.innerHTML = "" }
  }
}
