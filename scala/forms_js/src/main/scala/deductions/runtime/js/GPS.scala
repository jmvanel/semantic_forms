package deductions.runtime.js

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

import org.scalajs.dom
import org.scalajs.dom.raw.Geolocation
import org.scalajs.dom.raw.Position
import org.scalajs.dom.raw.PositionError

@JSExportTopLevel("GPS")
object GPS {

  /** obtain longitude & latitude from HTML5 API
   * cf http://stackoverflow.com/questions/40483880/geolocation-in-scala-js
   */
  def geoLocation(): Option[(Double, Double)] = {
    val window = dom.document.defaultView
    val nav = window.navigator
    val geo: Geolocation = nav.geolocation
    var longitude, latitude = 0.0

    def onSuccess(p: Position) = {
      longitude = p.coords.longitude
      latitude = p.coords.latitude
      println(s"latitude=${p.coords.latitude}")
      println(s"longitude=${p.coords.longitude}")
    }

    def onError(p: PositionError) = println("geoLocation: Error")

    geo.getCurrentPosition(onSuccess _, onError _)

    if(longitude ==0 && latitude ==0)
      None 
    else
      Some(longitude, latitude)
  }

}