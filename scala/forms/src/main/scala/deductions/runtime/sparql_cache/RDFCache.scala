package deductions.runtime.sparql_cache

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.apache.http.impl.cookie.DateUtils
import org.w3.banana.RDFModule
import org.w3.banana.RDFOpsModule
import org.w3.banana.RDFXMLReaderModule
import org.w3.banana.TurtleReaderModule
import org.w3.banana.XSDPrefix
import org.w3.banana.jena.JenaStore
import deductions.runtime.jena.JenaHelpers
import org.w3.banana.GraphStore
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
   * or download and store URI only if corresponding graph is empty
   * TODO according to timestamp retrieve from Jena Store,
   * TODO save in another Dataset
   */
  def retrieveURI(uri: Rdf#URI, store: JenaStore) = {
//      def storeURI(uri: Rdf#URI, store: GraphStore[Rdf]) {
    val uriGraphIsEmpty = store.readTransaction {
      val g = store.getGraph(uri)
      g.isEmpty()
    }
    println( "uriGraphIsEmpty " + uriGraphIsEmpty )
    if( uriGraphIsEmpty ) {
      storeURI(uri, store)
      println( "Graph at URI was downloaded: " + uri )
    }
  }

  /**
   * download and store URI in a graph named by itself,
   *  and store the timestamp from HTTP HEAD request
   */
  def storeURI(uri: Rdf#URI, store: JenaStore) {
//  def storeURI(uri: Rdf#URI, store: GraphStore[Rdf]) {
    val model = storeURI(uri, uri, store)
    val time = lastModified(uri.getURI(), 1000)
    store.writeTransaction {
      store.appendToGraph(makeUri(timestampGraphURI),
          Seq( makeTriple(
              uri,
              makeUri(timestampGraphURI),
              makeLiteral(time._2.toString, xsd.int ) ) ) )
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
        def tryHeaderField(headerName:String) : (Boolean, Boolean, Long ) = {
             val dateString = connection.getHeaderField(headerName)
             if( dateString != null ) {
               val date : java.util.Date = DateUtils.parseDate(dateString) // from apache http-components
               println( "responseCode: " + responseCode + " date " + date)
               (true, 200 <= responseCode && responseCode <= 399 , date.getTime() )
             } else (false, false, Long.MaxValue )
        }
        // TODO should be a better way in Scala:
        val lm = tryHeaderField("Last-Modified")
        val r = if( lm . _1 ) {
          ( lm . _2,  lm . _3 )
        } else {
          val lm2 = tryHeaderField("Date")
          if( lm2 . _1 ) {
            ( lm2 . _2,  lm2 . _3 )
          } else (false, Long.MaxValue)
        }
        return r 
    } catch {
          case exception: IOException => (false, Long.MinValue) 
          case e:Throwable => throw e
    }
   }
}

//object RDFCache extends RDFModule
//  with RDFOpsModule {
//   /** unused currently ... */
//  def getGraphURI(classs: Rdf#URI) : String = {
//    Ops.getFragment(classs) match {
//      case Some(frag) =>
////        classs.getURI().substring(frag.length() + 1)
//        classs.toString().substring(frag.length() + 1)
//      case None => ""
//    }
//  }
//}