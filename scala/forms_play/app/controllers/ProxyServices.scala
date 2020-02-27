package controllers

import play.api.mvc.Action

class ProxyServicesApp extends ProxyServices

trait ProxyServices  extends PlaySettings.MyControllerBase {

import org.apache.http.{HttpEntity, HttpResponse}
import org.apache.http.client._
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.params.HttpConnectionParams

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
def getRestContent(url: String,
                   connectionTimeout: Int    = 5000,
                   socketTimeout: Int    = 5000,
                   requestProperties: Map[String, Seq[String]] = Map(("Accept", Seq("application/json"))),
) = {
    val httpClient = buildHttpClient(connectionTimeout, socketTimeout)
    val httpRequest = new HttpGet(url)
    val headers = for (prop <- requestProperties ; property <- prop._2) yield (prop._1, property)
    val headersPassed = headers filter { case (key, value) =>
      Set("Accept", "Accept-Encoding", "Accept-Language") contains key }
    for ( header <- headersPassed ) {
      httpRequest.addHeader(header._1, header._2)}
    val httpResponse = httpClient.execute(httpRequest)
    val entity = httpResponse.getEntity
    val content = if (entity != null) {
        val inputStream = entity.getContent
        val byteArray = org.apache.commons.io.IOUtils.toByteArray(inputStream)
        if (inputStream != null) inputStream.close
        byteArray
    } else Array[Byte]()
    httpClient.getConnectionManager.shutdown
    (content, httpResponse)
}

private def buildHttpClient(connectionTimeout: Int, socketTimeout: Int):
DefaultHttpClient = {
    val httpClient = new DefaultHttpClient
    val httpParams = httpClient.getParams
    HttpConnectionParams.setConnectionTimeout(httpParams, connectionTimeout)
    HttpConnectionParams.setSoTimeout(httpParams, socketTimeout)
    httpClient.setParams(httpParams)
    httpClient
}


  private def getContentType(httpResponse: HttpResponse): String = {
    val contentTypeOption = for ( header <- httpResponse.getHeaders("Content-Type").headOption ) yield {
      header.getValue
    }
    contentTypeOption getOrElse ""
  }

  /** Proxy for HTTP get; used for HTTP images that need to be HTTPS;
   *  same for dbPedia Lookup */
  def getProxy(originalurl: String) = Action {
    implicit request =>
      val headers = request.headers.toMap
      val headersList = headers.toList
      val headersListFlat = for ( h <- headersList; header <- h._2 ; key  = h._1 ) yield {(key, header)}
      val (content, httpResponse) = getRestContent(originalurl, requestProperties=headers)
      val contentType = getContentType(httpResponse)
      Ok(content).withHeaders(headersListFlat:_*).as(contentType)
  }


  /**
   * Returns the text (content) from a REST URL as a String.
   * Inspired by http://matthewkwong.blogspot.com/2009/09/scala-scalaiosource-fromurl-blockshangs.html
   * and http://alvinalexander.com/blog/post/java/how-open-url-read-contents-httpurl-connection-java
   *
   * The `connectTimeout` and `readTimeout` comes from the Java URLConnection
   * class Javadoc.
   * @param url The full URL to connect to.
   * @param connectTimeout Sets a specified timeout value, in milliseconds,
   * to be used when opening a communications link to the resource referenced
   * by this URLConnection. If the timeout expires before the connection can
   * be established, a java.net.SocketTimeoutException
   * is raised. A timeout of zero is interpreted as an infinite timeout.
   * Defaults to 5000 ms.
   * @param readTimeout If the timeout expires before there is data available
   * for read, a java.net.SocketTimeoutException is raised. A timeout of zero
   * is interpreted as an infinite timeout. Defaults to 5000 ms.
   * @param requestMethod Defaults to "GET". (Other methods have not been tested.)
   *
   * @example get("http://www.example.com/getInfo")
   * @example get("http://www.example.com/getInfo", 5000)
   * @example get("http://www.example.com/getInfo", 5000, 5000)
   */
//  @throws(classOf[java.io.IOException])
//  @throws(classOf[java.net.SocketTimeoutException])
//  private def get(
//    url:            String,
//    connectTimeout: Int    = 5000,
//    readTimeout:    Int    = 5000,
//    requestProperties: Map[String, Seq[String]] = Map(("Accept", Seq("application/json"))),
//    requestMethod:  String = "GET") =
//  {
//    import java.net.{ URL, HttpURLConnection }
//    val connection = (new URL(url)).openConnection.asInstanceOf[HttpURLConnection]
//    connection.setConnectTimeout(connectTimeout)
//    connection.setReadTimeout(readTimeout)
//    connection.setRequestMethod(requestMethod)
//    for (prop <- requestProperties ; property <- prop._2)
//      connection.setRequestProperty(prop._1, property)
//    val inputStream = connection.getInputStream
//    val byteArray = org.apache.commons.io.IOUtils.toByteArray(inputStream)
//    if (inputStream != null) inputStream.close
//    (byteArray, connection)
//  }

//  def getProxy_old(originalurl: String) = Action {
//    implicit request =>
//      val headers = request.headers.toMap
//      val headersList = headers.toList
//      val headersListFlat = for ( h <- headersList; header <- h._2 ; key  = h._1 ) yield {(key, header)}
//      val (content, connection) = get(originalurl, requestProperties=headers)
//      Ok(content).withHeaders(headersListFlat:_*).as(connection.getContentType)
//  }
}