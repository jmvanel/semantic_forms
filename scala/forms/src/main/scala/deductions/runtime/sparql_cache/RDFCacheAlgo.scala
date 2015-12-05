package deductions.runtime.sparql_cache

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import org.apache.http.impl.cookie.DateUtils
import org.w3.banana.RDFModule
import org.w3.banana.RDFOpsModule
import org.w3.banana.RDFXMLReaderModule
import org.w3.banana.TurtleReaderModule
import org.w3.banana.XSDPrefix
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import deductions.runtime.dataset.RDFStoreLocalProvider
import org.w3.banana.OWLPrefix
import org.apache.log4j.Logger
import org.w3.banana.RDF
import org.w3.banana.jena.JenaModule
import java.math.BigInteger
import org.w3.banana.RDFStore
import org.w3.banana.RDFOps
import org.w3.banana.SparqlOps
import org.w3.banana.io.RDFReader
import org.w3.banana.io.Turtle
import org.w3.banana.io.RDFXML
import org.w3.banana.io.RDFLoader
import java.io.File
import deductions.runtime.services.SPARQLHelpers
import java.util.Date

//import java.net.URI

/** */
trait RDFCacheDependencies[Rdf <: RDF, DATASET] {
  implicit val turtleReader: RDFReader[Rdf, Try, Turtle]
  implicit val rdfXMLReader: RDFReader[Rdf, Try, RDFXML]
}

/** */
trait RDFCacheAlgo[Rdf <: RDF, DATASET] extends RDFStoreLocalProvider[Rdf, DATASET]
    with RDFCacheDependencies[Rdf, DATASET]
    with RDFLoader[Rdf, Try]
    with SPARQLHelpers[Rdf, DATASET] {

  import ops._
  import rdfStore.transactorSyntax._
  import rdfStore.graphStoreSyntax._
  import rdfStore.sparqlEngineSyntax._
  import scala.concurrent.ExecutionContext.Implicits.global

  val timestampGraphURI = "http://deductions-software.com/timestampGraph"
  lazy val xsd = XSDPrefix[Rdf]
  lazy val owl = OWLPrefix[Rdf]

  /** with transaction */
  def isGraphInUse(uri: String): Boolean = {
    isGraphInUse(makeUri(uri))
  }

  /** with transaction */
  def isGraphInUse(uri: Rdf#URI) = {
    dataset.r({
      for (graph <- dataset.getGraph(uri)) yield {
        val uriGraphIsEmpty = graph.size == 0
        println("uriGraphIsEmpty " + uriGraphIsEmpty)
        !uriGraphIsEmpty
      }
    }).flatMap { identity }.getOrElse(false)
  }

  /**
   * retrieve URI from a graph named by itself;
   * or download and store URI only if corresponding graph is empty,
   * with transaction
   */
  def retrieveURI(uri: Rdf#URI, dataset: DATASET): Try[Rdf#Graph] = {
    dataset.rw({
      retrieveURINoTransaction(uri: Rdf#URI, dataset: DATASET)
    }).flatMap { identity }
  }
  
  /**
   * retrieve URI from a graph named by itself;
   * or download and store URI only if corresponding graph is empty,
   * TODO save timestamp in another Dataset
   */
  def retrieveURINoTransaction(uri: Rdf#URI, dataset: DATASET): Try[Rdf#Graph] = {
      for (graph <- dataset.getGraph(uri)) yield {
        val uriGraphIsEmpty = graph.size == 0
        println(s"uriGraphIsEmpty: $uri : $uriGraphIsEmpty")
        if (uriGraphIsEmpty) {
          val g = storeURINoTransaction(uri, uri, dataset)
          println("Graph at URI was downloaded, new addition: " + uri + " , size " + g.size)
          addTimestampToDatasetNoTransaction(uri, dataset)
          g
        } else {
          updateLocalVersion(uri, dataset)
          graph
        }
      }
  }

  /**
   * according to timestamp download if outdated;
   * with transaction, in a Future
   */
  def updateLocalVersion(uri: Rdf#URI, dataset: DATASET) = {
    val future = Future {
      dataset.rw({
        val localTimestamp = getTimestampFromDataset(uri, dataset)
        localTimestamp match {
          case Success(longLocalTimestamp) => {
            println(s"$uri localTimestamp: ${	new Date(longLocalTimestamp) } .")
            val lastModifiedTuple = lastModified(uri.toString(), 500)
            println(s"$uri lastModified: $lastModifiedTuple.")
            
            if (lastModifiedTuple._1) {
            	if (lastModifiedTuple._2 > longLocalTimestamp ) {
            		storeURINoTransaction(uri, uri, dataset)
            		println(s"$uri was outdated by timestamp; downloaded.")
            		addTimestampToDatasetNoTransaction(uri, dataset)
            	}
            } else if (! lastModifiedTuple._1 || lastModifiedTuple._2 == Long.MaxValue ) {
              lastModifiedTuple._3 match {
                case Some(connection) => 
                val etag = headerField( fromUri(uri), "ETag": String, connection )
                val etagFromDataset = getETagFromDataset(uri, dataset)
                if(etag != etagFromDataset) {
                	storeURINoTransaction(uri, uri, dataset)
                  println(s"$uri was outdated by ETag; downloaded.")
                  // TODO store etag in TDB
                }
                case None =>
                storeURINoTransaction(uri, uri, dataset)
              }
            }
          }
          case Failure(fail) =>
            storeURINoTransaction(uri, uri, dataset)
            println(s"$uri had no localTimestamp ($fail); downloaded.")
        }
      })
    }
    future
  }

  /**
   * download and store URI content, with transaction, in a graph named by its URI minus the # part,
   *  and store the timestamp from HTTP HEAD request;
   * transactional,
   * load also the direct owl:imports , but not recursively ( as EulerGUI IDE does )
   */
  def storeContentInNamedGraph(uri: String): Rdf#Graph = {
    storeUriInNamedGraph(URI(uri))
  }

  /**
   * download and store URI content, with transaction, in a graph named by its URI minus the # part,
   *  and store the timestamp from HTTP HEAD request;
   * transactional,
   * load also the direct owl:imports , but not recursively ( as EulerGUI IDE does )
   */
  def storeUriInNamedGraph(uri: Rdf#URI): Rdf#Graph = {
    storeURI(uri)
  }

  /** NOTE: the dataset is provided by the parent trait;
   *  with transaction */
  private def storeURI(uri: Rdf#URI): Rdf#Graph = {
    val graphFromURI = storeURI(uri, uri, dataset)
    println("RDFCacheAlgo.storeURI " + uri + " size: " + graphFromURI.size)
    val r = dataset.rw({
      val it = find(graphFromURI, ANY, owl.imports, ANY)
      for (importedOntology <- it) {
        try {
          Logger.getRootLogger().info(s"Before Loading imported Ontology $importedOntology")
          foldNode(importedOntology.subject)(ontoMain => Some(ontoMain), x => None, x => None) match {
            case Some(ontoMain) =>
              foldNode(importedOntology.objectt)(onto => storeURINoTransaction(onto, onto, dataset),
                identity, identity)
            case None =>
          }
        } catch {
          case e: Throwable => println(e)
        }
      }
    })
    addTimestampToDataset(uri, dataset)
    graphFromURI
  }

  /**
   * add timestamp to dataset (actually a dedicated Graph timestampGraphURI ),
   *  with transaction
   */
  private def addTimestampToDataset(uri: Rdf#URI, dataset: DATASET) = {
    dataset.rw({
      addTimestampToDatasetNoTransaction(uri, dataset)
    })
  }

  /** replace Timestamp for URI to Dataset
   *  No Transaction */
  def addTimestampToDatasetNoTransaction(uri: Rdf#URI, dataset: DATASET) = {
        val queryString = s"""
         | DELETE {
         |   graph <$timestampGraphURI> {
         |     <$uri> <$timestampGraphURI> ?ts .
         |   }
         | } WHERE {
         |   graph <$timestampGraphURI> {
         |     <$uri> <$timestampGraphURI> ?ts .
         |   }
         | }""".stripMargin
    println( s"sparqlUpdate Query: $queryString")
    val res = sparqlUpdateQuery( queryString )
    println( s"sparqlUpdateQuery: $res")
    
    val time = lastModified(fromUri(uri), 1000)
    dataset.appendToGraph(makeUri(timestampGraphURI),
      makeGraph(Seq(makeTriple(
        uri,
        makeUri(timestampGraphURI),
        makeLiteral(time._2.toString, xsd.integer)))))
  }

  private def getETagFromDataset(uri: Rdf#URI, dataset: DATASET): String = {
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

  /**
   * get timestamp from dataset (actually a dedicated Graph timestampGraphURI ),
   *  No Transaction
   */
  private def getTimestampFromDataset(uri: Rdf#URI, dataset: DATASET): Try[Long] = {
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
  private def lastModified(url0: String, timeout: Int): (Boolean, Long, Option[HttpURLConnection]) = {
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
                ", date: " + date )
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

  private def headerField(url: String, headerName: String, connection: HttpURLConnection):
  String = {
    val headerString = connection.getHeaderField(headerName)
    if (headerString != null) {
      println("RDFCacheAlgo.tryHeaderField: " +
        s", header: $headerName = " + headerString +
        "; url: " + url)
        headerString
    } else ""
  }
  
  /**
   * store URI in a named graph,
   * transactional,
   * using Jena's RDFDataMgr
   * with Jena Riot for smart reading of any format,
   * (use content-type or else file extension)
   * cf https://github.com/w3c/banana-rdf/issues/105
   */
  def storeURI(uri: Rdf#URI, graphUri: Rdf#URI, dataset: DATASET): Rdf#Graph = {
    val r = dataset.rw({
      storeURINoTransaction(uri, graphUri, dataset)
    })
    r match {
      case Success(g) => g
      case Failure(e) =>
        Logger.getRootLogger().error("ERROR: " + e)
        throw e
    }
  }

  /**
   * read from uri no matter what the syntax is;
   * can also load an URI with the # part
   */
  def storeURINoTransaction(uri: Rdf#URI, graphUri: Rdf#URI, dataset: DATASET): Rdf#Graph = {
    Logger.getRootLogger().info(s"Before load uri $uri into graphUri $graphUri")
    if (fromUri(uri).startsWith("http") ||
      fromUri(uri).startsWith("ftp:") ||
      fromUri(uri).startsWith("file:")) {
      System.setProperty("sun.net.client.defaultReadTimeout", "10000")
      System.setProperty("sun.net.client.defaultConnectTimeout", "10000")
      val graph: Rdf#Graph =
        load(new java.net.URL(uri.toString())).get
      Logger.getRootLogger().info(s"Before storeURI uri $uri graphUri $graphUri")
      dataset.appendToGraph(graphUri, graph)
      Logger.getRootLogger().info(s"storeURI uri $uri : stored into graphUri $graphUri")
      graph
    } else {
      val message = s"Load uri $uri is not possible, not a downloadable URI."
      Logger.getRootLogger().warn(message)
      throw new Exception(message)
    }
  }
}

