package deductions.runtime.sparql_cache

import java.util.Date

import deductions.runtime.utils.RDFStoreLocalProvider
import deductions.runtime.utils.{HTTPHelpers, RDFHelpers, URIManagement}
import deductions.runtime.core.HTTPrequest
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpHead
import org.apache.http.client.{ClientProtocolException, ResponseHandler}
import org.apache.http.impl.client.HttpClients
import org.apache.log4j.Logger
import org.w3.banana._
//import deductions.runtime.sparql_cache.MicrodataLoaderModule
//import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.utils.Configuration
import org.w3.banana.io.{RDFReader, RDFXML, Turtle,RDFLoader}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.HttpClientBuilder
import org.w3.banana.jena.io.TripleSink
import org.apache.jena.riot.RDFParser
import org.w3.banana.io.JsonLd
import org.w3.banana.io.RDFWriter
import org.w3.banana.io.JsonLdCompacted
import java.net.URL

import scalaz._
import Scalaz._
import deductions.runtime.utils.StringHelpers
import scala.util.Success

/** implicit RDFReader's - TODO remove DATASET */
trait RDFCacheDependencies[Rdf <: RDF, DATASET] {
  val config: Configuration
  implicit val turtleReader: RDFReader[Rdf, Try, Turtle]
  implicit val rdfXMLReader: RDFReader[Rdf, Try, RDFXML]
  implicit val jsonldReader: RDFReader[Rdf, Try, JsonLd]

  implicit val rdfLoader: RDFLoader[Rdf, Try]

  implicit val turtleWriter: RDFWriter[Rdf, Try, Turtle]
  implicit val jsonldCompactedWriter: RDFWriter[Rdf, Try, JsonLdCompacted]
  implicit val rdfXMLWriter: RDFWriter[Rdf, Try, RDFXML]
}

/** */
trait RDFCacheAlgo[Rdf <: RDF, DATASET]
extends 
//RDFStoreLocalProvider[Rdf, DATASET]
//    with 
    RDFCacheDependencies[Rdf, DATASET]
    with MicrodataLoaderModule[Rdf]
    with TimestampManagement[Rdf, DATASET]
    with MirrorManagement[Rdf, DATASET]
    with BrowsableGraph[Rdf, DATASET]
    with RDFHelpers[Rdf]
    with URIManagement
    with HTTPHelpers
    with TypeAddition[Rdf, DATASET]
    with StringHelpers
  {

  import scala.concurrent.ExecutionContext.Implicits.global

  val activateMicrodataLoading = false // true
//  {
//    HttpOp.setDefaultHttpClient(HttpClients.createMinimal())
//    //	  logger.warn(
//    println(">>>> RDFCacheAlgo: setDefaultHttpClient(createMinimal())")
//  }

  import config._
  import ops._
  import rdfStore.transactorSyntax._

    lazy val xsd = XSDPrefix[Rdf]
  lazy val owl = OWLPrefix[Rdf]

  /** with transaction */
  def isGraphInUse(uri: String): Boolean = {
    isGraphInUse(makeUri(uri))
  }

  /** with transaction */
  def isGraphInUse(uri: Rdf#URI) = {
    rdfStore.r(dataset, {
      for (graph <- rdfStore.getGraph(dataset, uri)) yield {
        val uriGraphIsEmpty = graph.size === 0
        println("uriGraphIsEmpty " + uriGraphIsEmpty)
        !uriGraphIsEmpty
      }
    }).flatMap { identity }.getOrElse(false)
  }

  /**
   * retrieve URI from a graph named by itself;
   * or download and store URI, only if corresponding graph is empty,
   * with transaction
   */
  def retrieveURI(uri: Rdf#URI, dataset: DATASET = dataset): Try[Rdf#Graph] =
    retrieveURIBody(uri, dataset, HTTPrequest(), transactionsInside = true)

  /**
   * retrieve URI from a local graph in TDB named by the URI itself;
   * or download and store URI, only if corresponding graph is empty,
   * or local timestamp is older;
   * timestamp is saved in another Dataset
   *  @return the more recent RDF data from Internet if any, or the old data
   */
  def retrieveURIBody(uri: Rdf#URI, dataset: DATASET,
                      request: HTTPrequest,
                      transactionsInside: Boolean): Try[Rdf#Graph] = {

    val tryGraphLocallyManagedData = getLocallyManagedUrlAndData(uri, request, transactionsInside: Boolean)
//    println( s"retrieveURIBody: tryGraphLocallyManagedData $tryGraphLocallyManagedData")

    tryGraphLocallyManagedData match {
      case Some(tgr) => Success(tgr)
      case None =>

        val (nothingStoredLocally, graphStoredLocally) = checkIfNothingStoredLocally(uri, transactionsInside)

        val result =
          if (nothingStoredLocally) { // then read unconditionally from URI and store in TDB
          println(s"""retrieveURIBody: stored Graph Is Empty for URI <$uri>""")
          val mirrorURI = getMirrorURI(uri)
          val vv = if (mirrorURI === "") {
            val graphTry_MIME = readURI(uri, dataset, request)
            val graphDownloaded = {
              val graphTry = graphTry_MIME._1
              if (transactionsInside)
                storeURI(graphTry, uri, dataset)
              else
                storeURINoTransaction(graphTry, uri, dataset)
            }
            val vv = if (graphDownloaded.isSuccess) {
              println(s"""Graph at URI <$uri>, size ${graphDownloaded.get.size}
                    Either new addition was downloaded, or locally managed data""")
              addTimestampToDataset(uri, dataset2)
            } else
              println(s"Download Graph at URI <$uri> was tried, but it's faulty: $graphDownloaded")

            val contentType = graphTry_MIME._2
            println(s"""retrieveURINoTransaction: downloaded graph from URI <$uri> $graphDownloaded
                    size ${if (graphDownloaded.isSuccess) graphDownloaded.get.size} content Type: $contentType""")
            val ispureHTML = contentType.startsWith("text/html") // TODO case RDFa
            graphDownloaded match {
              case Success(gr) if (!ispureHTML) => Success(gr)
              case Success(gr) if (ispureHTML) =>
                // TODO pass transactionsInside
                Success(pureHTMLwebPageAnnotateAsDocument(uri, request))

              case Failure(f) => {
                println(s"Graph at URI <$uri> could not be downloaded, (exception ${f.getLocalizedMessage}, ${f.getClass} cause ${f.getCause}).")
                f match {
                  //case ex: ImplementationSettings.RDFReadException if (ex.getMessage().contains("text/html")) =>
                  case ex: Exception if (ex.getMessage().contains("text/html")) =>
                    /* Failure(org.apache.jena.riot.RiotException: Failed to determine the content type: (
                               URI=http://ihmia.afihm.org/programme/index.html : stream=text/html)) */

                    // TODO pass transactionsInside
                    Success(pureHTMLwebPageAnnotateAsDocument(uri, request))
                  case _ => graphDownloaded
                }
              }
            }
          } else {
            println(s"mirrorURI found: $mirrorURI")
            // TODO find in Mirror URI the relevant triples ( but currently AFAIK the graph returned by this function is not used )
            Success(emptyGraph)
          }
          vv
        } else { // something Stored Locally: get a chance for more recent RDF data, that will be shown next time
          Future {
            updateLocalVersion(uri, dataset).getOrElse(graphStoredLocally)
          }
          Success(graphStoredLocally)
        }
        result
    }
  }

  /**
   * check If Nothing Stored Locally
   *  @return stored Graph if any, and true iff nothing is Stored Locally
   */
  def checkIfNothingStoredLocally(
    uri:                Rdf#URI,
    transactionsInside: Boolean = true): (Boolean, Rdf#Graph) = {

    def doCheckIfNothingStoredLocally: (Boolean, Rdf#Graph) = {
      // if no triples <uri> ?P ?O , check graph named uri
      val tryGraphFromRdfStore = rdfStore.getGraph(dataset, uri)
      val sizeAndGraph =
        tryGraphFromRdfStore match {
          case Failure(f) =>
            println(s"""checkIfNothingStoredLocally: URI <$uri> : $f""")
            (0, emptyGraph)
          case Success(graphStoredLocally) => {
            (graphStoredLocally.size, graphStoredLocally)
          }
        }
      println(s"""checkIfNothingStoredLocally: TDB graph at URI <$uri> size ${sizeAndGraph._1}""")
      (sizeAndGraph._1 === 0, sizeAndGraph._2)
      //      nothingStoredAndGraph.getOrElse((false, emptyGraph))
    }

    if (transactionsInside) {
      val nothingStoredAndGraph = wrapInReadTransaction {
        doCheckIfNothingStoredLocally
      }
      if (nothingStoredAndGraph.isFailure)
        System.err.println(s"checkIfNothingStoredLocally: ${nothingStoredAndGraph}")
      nothingStoredAndGraph.getOrElse((false, emptyGraph))
    } else
      doCheckIfNothingStoredLocally
  }

  /**
   * according to stored and HTTP timestamps, download if outdated;
   * with NO transaction,
   * @return a graph with more recent RDF data or None
   */
  private def updateLocalVersion(uri: Rdf#URI, dataset: DATASET): Try[Rdf#Graph] = {
    val localTimestamp = dataset2.r { getTimestampFromDataset(uri, dataset2) }.get
    /* see http://stackoverflow.com/questions/5321876/which-one-to-use-expire-header-last-modified-header-or-etags
     * TODO: code too complex */
    localTimestamp match {
      case Success(longLocalTimestamp) => {
        println(s"updateLocalVersion: $uri local TDB Timestamp: ${new Date(longLocalTimestamp)} - $longLocalTimestamp .")
        val (noError, timestampFromHTTPHeader, connectionOption) = lastModified(uri.toString(), httpHeadTimeout)
        println(s"updateLocalVersion: <$uri> last Modified: ${new Date(timestampFromHTTPHeader)} - no Error: $noError .")

        if (isDocumentExpired(connectionOption)) {
          println(s"updateLocalVersion: <$uri> was OUTDATED by Expires HTPP header field")
          return readStoreURITry(uri, uri, dataset)
        }

        if (noError && (timestampFromHTTPHeader > longLocalTimestamp
          || longLocalTimestamp === Long.MaxValue)) {
          val graph = readStoreURITry(uri, uri, dataset)
          println(s"updateLocalVersion: <$uri> was OUTDATED by timestamp; downloaded.")

          // TODO pass arg. timestampFromHTTPHeader to addTimestampToDataset
          addTimestampToDataset(uri, dataset2) // PENDING: maybe do this in a Future

          graph
          //          } else Success(emptyGraph) // ????

        } else if (!noError ||
          timestampFromHTTPHeader === Long.MaxValue) {
          connectionOption match {
            case Some(connection) =>
              val etag = getHeaderField("ETag", connection)
              val etagFromDataset = dataset2.r { getETagFromDataset(uri, dataset2) }.get
              if (etag != etagFromDataset) {
                val graph = readStoreURITry(uri, uri, dataset)
                println(s"""updateLocalVersion: <$uri> was OUTDATED by ETag; downloaded.
                  etag "$etag" != etagFromDataset "$etagFromDataset" """)
                rdfStore.rw(dataset2, { addETagToDatasetNoTransaction(uri, etag, dataset2) })
                graph
              } else Success(emptyGraph)
            case None =>
              readStoreURITry(uri, uri, dataset)
          }
        } else Success(emptyGraph)
      }
      case Failure(fail) =>
        println(s"updateLocalVersion: <$uri> had no local Timestamp ($fail); download it:")
        readStoreURITry(uri, uri, dataset)
    }
  }

  /**
   * download and store URI content, with transaction, in a graph named by its URI minus the # part,
   *  and store the timestamp from HTTP HEAD request;
   * transactional,
   * load also the direct owl:imports , but not recursively ( as EulerGUI IDE does )
   */
  def readStoreUriContentInNamedGraph(uri: String): Rdf#Graph = {
    readStoreUriInNamedGraph(URI(uri))
  }

  /**
   * download and store URI content, with transaction, in a graph named by its URI minus the # part,
   *  and store the timestamp from HTTP HEAD request;
   * transactional,
   * load also the direct owl:imports , but not recursively ( as EulerGUI IDE does )
   */
  def readStoreUriInNamedGraph(uri: Rdf#URI): Rdf#Graph = {
    readStoreURIinOwnGraph(uri)
  }

  /**
   * store given URI in self graph; also store imported Ontologies by owl:imports
   *  with transaction
   */
  private def readStoreURIinOwnGraph(uri: Rdf#URI): Rdf#Graph = {
    val graphFromURI = readStoreURI(uri, uri, dataset)
    println("After RDFCacheAlgo.storeURI " + uri + " size: " + graphFromURI.size)
    wrapInTransaction {
      val it = find(graphFromURI, ANY, owl.imports, ANY)
      for (importedOntology <- it) {
        try {
          logger.info(s"Before Loading imported Ontology $importedOntology")
          foldNode(importedOntology.subject)(ontoMain => Some(ontoMain), _ => None, _ => None) match {
            case Some(uri /* : Rdf#URI */ ) =>
              foldNode(importedOntology.objectt)(onto => readStoreURINoTransaction(onto, onto, dataset),
                _ => emptyGraph,
                _ => emptyGraph); Unit
            case None => Unit
          }
        } catch {
          case e: Throwable => logger.error(e)
        }
      }
    }
    addTimestampToDataset(uri, dataset2)
    graphFromURI
  }

  /**
   * store URI in a named graph,
   * transactional,
   * using Jena's RDFDataMgr
   * with Jena Riot for smart reading of any format,
   * (use content-type or else file extension)
   * cf https://github.com/w3c/banana-rdf/issues/105
   */
  def readStoreURI(uri: Rdf#URI, graphUri: Rdf#URI, dataset: DATASET): Rdf#Graph = {
    val r = rdfStore.rw(dataset, {
      readStoreURINoTransaction(uri, graphUri, dataset)
    })
    r.flatten match {
      case Success(g) => g
      case Failure(e) =>
        Logger.getRootLogger().error("ERROR: " + e)
        throw e
    }
  }

  /**
   * read unconditionally from URI and store in TDB,
   * no matter what the concrete syntax is;
   * can also load an URI with the # part
   *
   * TODO: remove, to split read (no Transaction needed) & Store (Transaction needed)
   */
  private def readStoreURINoTransaction(uri: Rdf#URI, graphUri: Rdf#URI, dataset: DATASET,
                                        request: HTTPrequest = HTTPrequest()): Try[Rdf#Graph] = {
    val graphTry = readURI(uri, dataset, request) . _1
    storeURINoTransaction(graphTry, graphUri, dataset)
  }
  /** read unconditionally from URI and store in TDB, Transaction inside */
  private def readStoreURITry(uri: Rdf#URI, graphUri: Rdf#URI, dataset: DATASET,
                              request: HTTPrequest = HTTPrequest()): Try[Rdf#Graph] = {
    val graphTry = readURI(uri, dataset, request) . _1
    storeURI(graphTry, graphUri, dataset)
  }

  /** store URI in TDB, including Transaction */
  def storeURI(
    graphTry: Try[Rdf#Graph], graphUri: Rdf#URI, dataset: DATASET): Try[Rdf#Graph] = {
    wrapInTransaction {
      storeURINoTransaction(graphTry, graphUri, dataset)
    }.flatten
  }

  private def storeURINoTransaction(
    graphTry: Try[Rdf#Graph], graphUri: Rdf#URI, dataset: DATASET): Try[Rdf#Graph] = {
    logger.info(s"readStoreURINoTransaction: Before appendToGraph graphUri <$graphUri>")
    if (graphTry.isSuccess) rdfStore.appendToGraph(dataset, graphUri, graphTry.get)
    else
      println(s"storeURINoTransaction: $graphTry")
    // TODO: reuse appendToGraph return
    logger.info(s"readStoreURINoTransaction: stored into graphUri <$graphUri>")
    graphTry
  }

  /**
   * read unconditionally from URI,
   * no matter what the concrete syntax is
   */
  private def readURI(
    uri:     Rdf#URI,
    dataset: DATASET,
    request: HTTPrequest) = {
    readURIsf( uri, dataset, request)
    // reactivated; fixed problem at /login
//    readURIWithJenaRdfLoader( uri, dataset, request)
  }

  /** for downloading RDF uses Jena RDFDataMgr,
   *  and activate Microdata Loading */
  private def readURIWithJenaRdfLoader(
      uri: Rdf#URI,
      dataset: DATASET,
      request: HTTPrequest ): (Try[Rdf#Graph], String) = {
//    Logger.getRootLogger().info(s"Before load uri $uri into graphUri $graphUri")
    Logger.getRootLogger().info(s"Before load uri $uri")

    if (isDownloadableURI(uri)) {
      // To avoid troubles with Jena cf https://issues.apache.org/jira/browse/JENA-1335
      val contentType = getContentTypeFromHEADRequest(fromUri(uri))
    	println(s""">>>> readURIWithJenaRdfLoader: getContentTypeFromHEADRequest: contentType for <$uri> "$contentType" """)
      if (
//          !contentType.startsWith("text/html") &&
          !contentType.startsWith("ERROR") ) {
        setTimeoutsFromConfig()
        // NOTE: Jena RDF loader can throw an exception "Failed to determine the content type"
        val graphTryLoadedFromURL = rdfLoader.load(new java.net.URL(withoutFragment(uri).toString()))
//        logger.info
        println(s"readURI: after rdfLoader.load($uri): $graphTryLoadedFromURL")

        graphTryLoadedFromURL match {

        case Success(gr) =>
            (graphTryLoadedFromURL, contentType)

          case Failure(f) =>
            println(s""">>>> readURIWithJenaRdfLoader: Failed with Jena RDF loader for <$uri>
               trying read With explicit content Type; ContentType From HEAD Request "$contentType" """)
            // after Java-RDFa is updated to latest Jena
            val graphFromMicrodata = graphTryLoadedFromURL.getOrElse {
              if (activateMicrodataLoading) microdataLoading(uri) else emptyGraph }

//            if( contentType != "ERROR" ) {
            /* NOTE: hoping that Jena > 3.4.0 will solve all issues on RDFDataMgr,
             * but before that , we try this */
            val gr =
              if(graphFromMicrodata.size == 0)
                readWithContentType( uri, contentType, dataset): Try[Rdf#Graph]
              else
                Success(graphFromMicrodata)
            println(s"""readURIWithJenaRdfLoader After readWithContentType: ${gr}""")
            ( gr, contentType)
        }

      } else {
        (Success(emptyGraph), contentType)
      }
    } else {
      val message = s"Load uri <$uri> is not possible, not a downloadable URI."
      logger.warn(message)
      ( Success(emptyGraph), "" ) // TODO return Failure( new Exception("") )
    }
  }

  /**
   * read unconditionally from URI,
   * no matter what the concrete syntax is;
   * use SF specific implementation for downloading RDF, not Jena RDFDataMgr
   * TODO:
   * - also load an URI with the # part
   * - load a file: URI
   */
  private def readURIsf(
      uri: Rdf#URI,
      dataset: DATASET,
      request: HTTPrequest ): (Try[Rdf#Graph], String) = {
//    Logger.getRootLogger().info(s"Before load uri $uri into graphUri $graphUri")
    Logger.getRootLogger().info(s"Before load uri $uri")

    if (isDownloadableURI(uri)) {
      // To avoid troubles with Jena cf https://issues.apache.org/jira/browse/JENA-1335
      val contentType = getContentTypeFromHEADRequest(fromUri(withoutFragment(uri)))
    	println(s""">>>> readURI: getContentTypeFromHEADRequest: contentType for <$uri> "$contentType" """)
      if (!contentType.startsWith("text/html") &&
          !contentType.startsWith("ERROR") ) {
            println(s""">>>> readURIsf: for <$uri>
               trying read With explicit content Type; ContentType From HEAD Request "$contentType" """)
            val gr = readWithContentType( uri, contentType, dataset): Try[Rdf#Graph]
            println(s"""readURIsf After readWithContentType: ${gr}""")
            ( gr, contentType)

      } else {
        (Success(emptyGraph), contentType)
      }
    } else {
      val message = s"Load uri <$uri> is not possible, not a downloadable URI."
      logger.warn(message)
      ( Success(emptyGraph), "" ) // TODO return Failure( new Exception("") )
    }
  }

  /** pasted from Apache HTTP client doc
   *  https://hc.apache.org/httpcomponents-client-ga/
   * */
  def getContentTypeFromHEADRequest(url0: String): String = {
    val url = url0 // TODO: test more: fromUri(withoutFragment(URI(url0)))
    val requestConfig = RequestConfig.custom().setConnectTimeout(5 * 1000).build();
    val httpclient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
    try {
      val httpHead = new HttpHead(url)
      // TODO somehow reuse trait RDFContentNegociation
      httpHead.setHeader(
          "Accept",
          "application/rdf+xml, text/turtle; charset=utf-8, application/ld+json; charset=utf-8")

      System.out.println("Executing request " + httpHead.getRequestLine());
      // Create a custom response handler
      val responseHandler = new ResponseHandler[String]() {
        override
        def handleResponse(response: HttpResponse):String = {
          val status = response.getStatusLine().getStatusCode();
          if (
              (status >= 200 && status < 300) ||
              status === 406 ||status === 404 // Not Acceptable
              ) {
            val ct = response.getFirstHeader("Content-Type")
            if ( ct == null ) "" else ct.getValue
          } else {
            System.err.println(s"---- ${response.getStatusLine()}");
            throw new ClientProtocolException(
                s"getContentTypeFromHEADRequest: Unexpected response status: $status");
          }
        }
      };
      val responseHandled =
        if (!url.startsWith("file:")) // NOTE: necessary for deductions.runtime.jena.TestJenaHelpers
          httpclient.execute(httpHead, responseHandler)
        else
          ""
      System.out.println(s"getContentTypeFromHEADRequest: $responseHandled");
      responseHandled
    }
    catch {
      case t: Throwable =>
        System.err.println(s"getContentTypeFromHEADRequest($url): ${t.getLocalizedMessage}")
        "ERROR"
    }
    finally {
      httpclient.close();
    }
  }
  /** unused function : commented for modularization */
  private def microdataLoading(uri: Rdf#URI): Rdf#Graph = {
    logger.info(s"Trying RDFa for <$uri>")
    microdataLoader.load(
      new java.net.URL(withoutFragment(uri).toString())) match {
        case Success(s) => s
        case Failure(f) => {
          logger.error("microdataLoading: START MESSAGE")
          logger.error(f.getMessage)
          logger.error(s""" uri.toString.contains("/ldp/") ${uri.toString.contains("/ldp/")} """)
          logger.error("END MESSAGE")
          // catch only "pure" HTML web page: TODO? make a function isPureHTMLwebPage(uri: URI, request: Request): Boolean
          //              if (f.getMessage.contains("Failed to determine the content type:")) {
          //                logger.info(s"<$uri> is a pure HTML web page (no RDFa or microformats");
          //                val tryGraph = getLocallyManagedUrlAndData(uri, request)
          //                tryGraph . get
          //              } else
          // throw f
          emptyGraph
        }
      }
  }

  /* test if given URI is a locally managed URL, that is created locally and 100% located here */
  /** get Locally Managed graph from given URI : <URI> ?P ?O */
  private def getLocallyManagedUrlAndData(uri: Rdf#URI, request: HTTPrequest, transactionsInside: Boolean): Option[Rdf#Graph] =
    if (!fromUri(uri).startsWith(request.absoluteURL(""))) { // TODO ? "ldp"
      // then it can be a "pure" HTML web page, or an RDF document
      None
    } else {
      // it's a locally managed user URL and data, no need to download anything
      // used by formatHTMLStatistics():
      // TODO test rather: Some(rdfStore.getGraph( dataset, uri ).getOrElse(emptyGraph))

      val gr1 =
        if (transactionsInside)
          wrapInReadTransaction { makeGraph(find(allNamedGraph, uri, ANY, ANY).toIterable) }
        else
          Success(makeGraph(find(allNamedGraph, uri, ANY, ANY).toIterable))

      gr1 match {
        case Success(gr) => Some(gr)
        case Failure(f) =>
          logger.error(s"getLocallyManagedUrlAndData: $f")
          None
      }
    }

  /**
   * transaction inside (Write)
   * TODO: graphURI should be obtained from the HTTP request, or else from user Id
   */
  private def pureHTMLwebPageAnnotateAsDocument(uri: Rdf#URI, request: HTTPrequest): Rdf#Graph = {
    val graphURI = URI(makeAbsoluteURIForSaving(request.userId()))
    val addedGraphTry = wrapInTransaction {
      val existingType = find(allNamedGraph, uri, rdf.typ, ANY)
      val addedGraph =
        if (existingType.isEmpty) {
          val label = substringAfterLastIndexOf(fromUri(uri), "/" ) .
            getOrElse(fromUri(uri)) .
            replace("-", " ") .
            replace(".html", " ")
          val newGraphWithUrl = makeGraph(List(
              makeTriple(uri, rdf.typ, foaf.Document),
              makeTriple(uri, rdfs.label, Literal(label))
          ))
          rdfStore.appendToGraph(
            dataset,
            graphURI,
            newGraphWithUrl)
          println(s"""pureHTMLwebPageAnnotateAsDocument: saved $newGraphWithUrl
            in graph <${makeAbsoluteURIForSaving(request.userId())}>""")
          newGraphWithUrl
        } else emptyGraph
      addRDFSLabelValue(uri, Some(graphURI))
      addedGraph
    }
    val currentPageTriplesIterator = wrapInReadTransaction {
      find(allNamedGraph, uri, ANY, ANY)
    }.getOrElse(Iterator.empty).toIterable
    println(s"""pureHTMLwebPageAnnotateAsDocument: addedGraphTry $addedGraphTry""")
    val result = addedGraphTry.getOrElse(emptyGraph). // newGraphWithUrl.
      // NOTE: after user added triples, this way typeChange will not be triggered
      union(makeGraph(currentPageTriplesIterator))
    println(s"pureHTMLwebPageAnnotateAsDocument: ret $result")
    result
  }
}
