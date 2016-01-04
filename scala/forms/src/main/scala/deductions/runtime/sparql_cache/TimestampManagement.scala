package deductions.runtime.sparql_cache

import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

import scala.concurrent.ExecutionContext.Implicits
import scala.util.Success
import scala.util.Try

import org.apache.http.impl.cookie.DateUtils
import org.w3.banana.RDF

import deductions.runtime.dataset.RDFStoreLocalProvider
import deductions.runtime.services.SPARQLHelpers

trait TimestampManagement[Rdf <: RDF, DATASET]
extends RDFStoreLocalProvider[Rdf, DATASET]
    with SPARQLHelpers[Rdf, DATASET] {

  val timestampGraphURI = "http://deductions-software.com/timestampGraph"

  import ops._
  import rdfStore.transactorSyntax._
  import rdfStore.graphStoreSyntax._
  import rdfStore.sparqlEngineSyntax._
  import scala.concurrent.ExecutionContext.Implicits.global

  /**
   * add timestamp to dataset (actually a dedicated Graph timestampGraphURI ),
   *  with transaction
   */
  def addTimestampToDataset(uri: Rdf#URI, dataset: DATASET) = {
    dataset.rw({
      addTimestampToDatasetNoTransaction(uri, dataset)
    })
  }

  /** replace Timestamp for URI to Dataset
   *  No Transaction */
  def addTimestampToDatasetNoTransaction(uri: Rdf#URI, dataset: DATASET) = {
	  val time = lastModified(fromUri(uri), 1000)
	  println("addTimestampToDatasetNoTransaction: " + time._2 )
    replaceRDFTriple(
      makeTriple(
        uri,
        makeUri(timestampGraphURI),
        makeLiteral(time._2.toString, xsd.integer)),
      URI(timestampGraphURI),
      dataset)
  }

    /**
   * get timestamp from dataset (actually a dedicated Graph timestampGraphURI ),
   *  No Transaction
   */
  def getTimestampFromDataset(uri: Rdf#URI, dataset: DATASET): Try[Long] = {
    import org.w3.banana.binder._
    import java.math.BigInteger
    val queryString = s"""
         |SELECT DISTINCT ?ts WHERE {
         |  graph <$timestampGraphURI> {
         |    <$uri> <$timestampGraphURI> ?ts .
         |  }
         |}""".stripMargin
    val result = for {
      query <- sparqlOps.parseSelect(queryString)
      solutions <- dataset.executeSelect(query, Map())
    } yield {
      solutions.toIterable.map {
        row =>
          {
            val node = row("ts").get
            val r1 = foldNode(node)(
              _ => Success(new BigInteger("0")),
              _ => Success(new BigInteger("0")),
              lit => lit.as[BigInteger])
            r1.get
            // getOrElse sys.error("getTimestampFromDataset: " + row)
          }
      }
    }
    result.map { x => x.next.longValue() }
  }
  
  /** get lastModified HTTP header
   * @return pair:
   * _1 : true <=> no error
   * _2 : timestamp from HTTP HEAD request or local file;
   * return Long.MaxValue if no timestamp is available;
   *  NOTE elsewhere akka HTTP client is used
   */
  def lastModified(url0: String, timeout: Int): (Boolean, Long, Option[HttpURLConnection]) = {
    val url = url0.replaceFirst("https", "http"); // Otherwise an exception may be thrown on invalid SSL certificates.
    try {
      val connection0 = new URL(url).openConnection()
      connection0 match {
        case connection: HttpURLConnection =>
          connection.setConnectTimeout(timeout);
          connection.setReadTimeout(timeout);
          connection.setRequestMethod("HEAD");
          val responseCode = connection.getResponseCode()

          def tryHeaderField(headerName: String): (Boolean, Boolean, Long) = {
            val dateString = headerField(url, headerName, connection)
            if (dateString != "") {
              val date: java.util.Date = DateUtils.parseDate(dateString) // from apache http-components
              println("RDFCacheAlgo.lastModified: responseCode: " + responseCode +
                ", date: " + date + ", dateString " + dateString)
              (true, 200 <= responseCode && responseCode <= 399, date.getTime())
            } else (false, false, Long.MaxValue)
          }
          
          val lm = tryHeaderField("Last-Modified")
          val r = if (lm._1) {
            (lm._2, lm._3, Some(connection) )
          } else (false, Long.MaxValue, Some(connection) )
          return r

        case _ if(url.startsWith("file:/") ) =>
          val f = new File( new java.net.URI(url) )
          (true,  f.lastModified(), None )
          
        case _ =>
          println( s"lastModified(): Case not implemented: $url - $connection0")
          (false, Long.MaxValue, None )

      }
    } catch {
      case exception: IOException =>
        println(s"lastModified($url0")
//        logger.warn("")
        (false, Long.MaxValue, None)
      case e: Throwable           => throw e
    }
  }

  def headerField(url: String, headerName: String, connection: HttpURLConnection):
  String = {
    val headerString = connection.getHeaderField(headerName)
    if (headerString != null) {
      println("RDFCacheAlgo.tryHeaderField: " +
        s", header: $headerName = " + headerString +
        "; url: " + url)
        headerString
    } else ""
  }

  //// ETag ////
  
  def getETagFromDataset(uri: Rdf#URI, dataset: DATASET): String = {
	  val queryString = s"""
         |SELECT DISTINCT ?ts WHERE {
         |  GRAPH <$timestampGraphURI> {
         |    <$uri> <ETag> ?etag .
         |  }
         |}""".stripMargin
    val list = sparqlSelectQueryVariables(queryString, Seq("etag") )
    val v = list.headOption.getOrElse(Seq())
    val vv = v.headOption.getOrElse(Literal(""))
    vv.toString()
  }

  def addETagToDatasetNoTransaction(uri: Rdf#URI, etag: String, dataset: DATASET) = {
    replaceRDFTriple(
      makeTriple(
        uri,
        makeUri("ETag"),
        makeLiteral( etag, xsd.string )),
      URI(timestampGraphURI),
      dataset)
  }

}