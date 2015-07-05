package models

import play.api.Play.current
import play.api.libs.ws._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import akka.util.Timeout
import scala.concurrent.duration._
import java.net.URLEncoder
import scala.concurrent.Await

/**
 * @author jmv
 */

/**
 * cf https://www.playframework.com/documentation/2.3.x/ScalaWS
 *  http://stackoverflow.com/questions/24881145/how-do-i-use-play-ws-library-in-normal-sbt-project-instead-of-play
 */
object WSClient extends App {

   println( fetchLatitudeAndLongitude("Lyon") )
   //  testWithPlayRunning()

  /**
   * cf http://blog.knoldus.com/2013/03/25/getting-longitude-latitude-for-a-address-using-play-framework-2-1-ws-api/
   */
  def fetchLatitudeAndLongitude(address: String): Option[(Double, Double)] = {
    val builder = new com.ning.http.client.AsyncHttpClientConfig.Builder()
    val client = new play.api.libs.ws.ning.NingWSClient(builder.build())

    implicit val timeout = Timeout(50000 milliseconds)

    // Encoded the address in order to remove the spaces from the address (spaces will be replaced by '+')
    //@purpose There should be no spaces in the parameter values for a GET request
    val addressEncoded = URLEncoder.encode(address, "UTF-8")
    val url = "http://maps.googleapis.com/maps/api/geocode/json?address=" +
      addressEncoded + "&sensor=true"
    val response = client.url(url).get()
    //    val response = WS.url(url).get()

    val future = response map {
      response => (response.json \\ "location")
    }

    // Wait until the future completes (Specified the timeout above)
    val result = Await.result(future, timeout.duration).asInstanceOf[List[play.api.libs.json.JsObject]]

    //Fetch the values for Latitude & Longitude from the result of future
    val latitude = (result(0) \\ "lat")(0).toString.toDouble
    val longitude = (result(0) \\ "lng")(0).toString.toDouble
    Option(latitude, longitude)
  }

  def testWithPlayRunning() {
    val url = "http://maps.googleapis.com/maps/api/geocode/json?address=NewDelhi&sensor=true"
    val holder: WSRequestHolder = WS.url(url)
    // This returns a WSRequestHolder that you can use to specify various HTTP options, such as setting headers. You can chain calls together to construct complex requests.

    val complexHolder: WSRequestHolder =
      holder.withHeaders("Accept" -> "application/json")
        .withRequestTimeout(10000)
        .withQueryString("search" -> "play")

    val futureResponse: Future[WSResponse] = complexHolder.get()
    implicit val timeout = Timeout(50000 milliseconds)
    futureResponse.onSuccess {
      case resp => println(resp)
    }
    Await.result(futureResponse, timeout.duration)
  }
}