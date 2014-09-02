package deductions.runtime.sparql_cache

import org.w3.banana.RDFModule
import org.w3.banana.RDFOpsModule
import org.w3.banana.RDFXMLReaderModule
import org.w3.banana.TurtleReaderModule
import org.w3.banana.jena.JenaStore
import deductions.runtime.jena.JenaHelpers
import deductions.runtime.jena.RDFStoreObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.IOException
import org.apache.http.impl.cookie.DateUtils
import org.w3.banana.XSDPrefix
//import org.apache.http.client.utils.DateUtils

/** */
trait RDFCache
  extends RDFModule
  with RDFOpsModule
  with TurtleReaderModule
  with RDFXMLReaderModule {
}

// TODO depend on generic Rdf
trait RDFCacheJena extends RDFCache with JenaHelpers {

  val timestampGraphURI = "http://deductions-software.com/timestampGraph"
  val xsd = XSDPrefix[Rdf]
  import Ops._

  /**
   * retrieve URI from a graph named by itself;
   * or download and store URI if such a graph is empty
   * TODO according to timestamp retrieve from Jena Store,
   */
  def retrieveURI(uri: Rdf#URI, store: JenaStore) = {
    val uriGraphIsEmpty = store.readTransaction {
      val g = store.getGraph(uri)
      g.isEmpty()
    }
    if( uriGraphIsEmpty ) storeURI(uri, store)
  }

  /**
   * download and store URI in a graph named by itself,
   *  and store the timestamp from HTTP HEAD request
   */
  def storeURI(uri: Rdf#URI, store: JenaStore) {
    storeURI(uri, uri, store)
//    val u = uri.toString
    val time = lastModified(uri.getURI(), 1000)
    store.writeTransaction {
      store.appendToGraph(makeUri(timestampGraphURI),
          Seq( makeTriple(
              uri,
              makeUri(timestampGraphURI),
              makeLiteral(time.toString, xsd.int ) ) ) )
    }
  }

   def lastModified( url0:String, timeout:Int) : (Boolean, Long) = {
    val url = url0.replaceFirst("https", "http"); // Otherwise an exception may be thrown on invalid SSL certificates.

    try {
        val connection0 = new URL(url).openConnection()
        val connection = connection0.asInstanceOf[HttpURLConnection] 
        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout);
        connection.setRequestMethod("HEAD");
        val responseCode = connection.getResponseCode();
        val dateString = connection.getHeaderField("Last-Modified")
        val date : java.util.Date = DateUtils.parseDate(dateString) // from apache http-components
        
        (200 <= responseCode && responseCode <= 399 , date.getTime() )
    } catch {
    case exception: IOException => (false, Long.MinValue) 
    case e:Throwable => throw e
    }
   }
}