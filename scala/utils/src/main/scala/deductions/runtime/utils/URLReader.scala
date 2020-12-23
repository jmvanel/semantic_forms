package deductions.runtime.utils

import java.net.URL

/** cf https://alvinalexander.com/scala/how-to-write-scala-http-get-request-client-source-fromurl/ */
trait URLReader {
  import java.io._
  import org.apache.http.{ HttpEntity, HttpResponse }
  import org.apache.http.client._
  import org.apache.http.client.methods.HttpGet
  import org.apache.http.impl.client.DefaultHttpClient
  import scala.collection.mutable.StringBuilder
  import scala.xml.XML
  import org.apache.http.params.HttpConnectionParams
  import org.apache.http.params.HttpParams

  /**
   * Returns the text (content) from a REST URL as a String.
   * Returns a blank String if there was a problem.
   * This function will also throw exceptions if there are problems trying
   * to connect to the url.
   *
   * @param url A complete URL, such as "http://foo.com/bar"
   * @param connectionTimeout The connection timeout, in ms.
   * @param socketTimeout The socket timeout, in ms.
   */
  def getRestContent(
    url:               String,
    connectionTimeout: Int = 3000,
    socketTimeout:     Int= 3000): String = {
    val httpClient = buildHttpClient(connectionTimeout, socketTimeout)
    val httpResponse = httpClient.execute(new HttpGet(url))
    val entity = httpResponse.getEntity
    var content = ""
    if (entity != null) {
      val inputStream = entity.getContent
      content = io.Source.fromInputStream(inputStream).getLines.mkString
      inputStream.close
    }
    httpClient.getConnectionManager.shutdown
    content
  }

  def getRestInputStream(
    url:               String,
    connectionTimeout: Int    = 5000,
    socketTimeout:     Int    = 3000): Option[InputStream] = {
    if ( url.startsWith("file:") ) {
      //      new FileInputStream( url.replace("^file:", "") )
      logger.info(s"getRestContent: URL <$url> is file")
      return Some(new URL(url).openStream())
    } else {
      val httpClient = buildHttpClient(connectionTimeout, socketTimeout)
      val httpGeturl = new HttpGet(url)

      // For GeoNature
//      httpGeturl setHeader (
//        "Cookie",
//        "token=eyJhbGciOiJIUzI1NiIsImlhdCI6MTU5NjcwMjM4MywiZXhwIjoxNTk3MzA3MTgzfQ.eyJpZF9yb2xlIjoxLCJub21fcm9sZSI6IkFkbWluaXN0cmF0ZXVyIiwicHJlbm9tX3JvbGUiOiJ0ZXN0IiwiaWRfYXBwbGljYXRpb24iOjMsImlkX29yZ2FuaXNtZSI6LTEsImlkZW50aWZpYW50IjoiYWRtaW4iLCJpZF9kcm9pdF9tYXgiOjF9.4mbwrP9iQ8gTHewWLvJMhl1NiFHnP2C_UQgiMBrni0o; session=.eJyrVvJ3dg5xjFCyqlYqLU4tik8uKi1LTQFxnZWslIyVdJRcoLQrlA6C0qFQOgxM19bWAgAPNhKx.Eg1atA.kvCblUrgeP-yZJ8RyZaS5eNf_PY")

      val httpResponse = httpClient.execute(httpGeturl)
      logger.info(s"getRestContent: $url : StatusLine ${httpResponse.getStatusLine}")
      val entity = httpResponse.getEntity
      return if (entity != null) {
        val inputStream = entity.getContent
        Some(inputStream)
      } else
        None
      // httpClient.getConnectionManager.shutdown
    }
  }

  private def buildHttpClient(connectionTimeout: Int, socketTimeout: Int): DefaultHttpClient = {
    val httpClient = new DefaultHttpClient
    val httpParams = httpClient.getParams
    HttpConnectionParams.setConnectionTimeout(httpParams, connectionTimeout)
    HttpConnectionParams.setSoTimeout(httpParams, socketTimeout)
    httpClient.setParams(httpParams)
    httpClient
  }
}