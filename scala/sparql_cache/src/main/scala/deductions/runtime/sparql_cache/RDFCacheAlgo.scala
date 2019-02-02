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
//    logger.debug(">>>> RDFCacheAlgo: setDefaultHttpClient(createMinimal())")
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
        logger.debug("uriGraphIsEmpty " + uriGraphIsEmpty)
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
                      request:            HTTPrequest,
                      transactionsInside: Boolean): Try[Rdf#Graph] = {
    val ret = retrieveURIResourceStatus(uri, dataset,
      request: HTTPrequest, transactionsInside)
    ret._1
  }

  /** retrieve URI with Resource Status
   * TODO [Marco] collect possible behavior patterns
to give more meaningful feedback to user
1. site is down
2. rdf incorrect
3. server response but no triples
etc
JMV:
- probably need to leverage in class inheritance on Java exceptions
- On Internet data, I want to store more: HTTP headers like Last-updated, number of triples, checksum, user who did the loading, what more ?
 */
  def retrieveURIResourceStatus(uri: Rdf#URI, dataset: DATASET,
                      request: HTTPrequest,
                      transactionsInside: Boolean): (Try[Rdf#Graph], Try[String]) = {
    val tryGraphLocallyManagedData = getLocallyManagedUrlAndData(uri, request, transactionsInside: Boolean)

//    logger.debug(
    logger.debug(  "LOADING " + s"retrieveURIBody: tryGraphLocallyManagedData $tryGraphLocallyManagedData")

    tryGraphLocallyManagedData match {
      case Some(tgr) => (Success(tgr), Success(""))
      case None =>

        val (nothingStoredLocally, graphStoredLocally) = checkIfNothingStoredLocally(uri, transactionsInside)

        val result: (Try[Rdf#Graph], Try[String]) =
          if (nothingStoredLocally) { // then read unconditionally from URI and store in TDB
          logger.debug(s"""retrieveURIBody: stored Graph Is Empty for URI <$uri>""")
          val mirrorURI = getMirrorURI(uri)
          val resultWhenNothingStoredLocally: (Try[Rdf#Graph], Try[String]) =
            if (mirrorURI === "") {
            val graphTry_MIME = readURI(uri, dataset, request)
            logger.debug(  "LOADING " + s""">>>> retrieveURIBody graphTry_MIME $graphTry_MIME""")
            val graphDownloaded = {
              val graphTry = graphTry_MIME._1
              if (transactionsInside)
                storeURI(graphTry, uri, dataset)
              else
                storeURINoTransaction(graphTry, uri, dataset)
            }
            if (graphDownloaded.isSuccess) {
//              logger.debug(
              logger.debug(  "LOADING " +
                  s"""Graph at URI <$uri>, size ${graphDownloaded.get.size}
                    Either new addition was downloaded, or locally managed data""")
              addTimestampToDataset(uri, dataset2)
              //, Success("") )
              } else
              logger.error(
                s"Download Graph at URI <$uri> was tried, but it's faulty: $graphDownloaded")

            val contentType = graphTry_MIME._2
            logger.debug(s"""retrieveURIBody: downloaded graph from URI <$uri> $graphDownloaded
                    size ${if (graphDownloaded.isSuccess) graphDownloaded.get.size} content Type: $contentType""")
            val isDocument = isDocumentMIME(contentType) // TODO case RDFa
            graphDownloaded match {
              case Success(gr) if (!isDocument) => (Success(gr), Success(""))
              case Success(gr) if (isDocument) =>
                // TODO pass transactionsInside
                (Success(pureHTMLwebPageAnnotateAsDocument(uri, request)), Success(""))

              case Failure(f) => {
                logger.debug(s"Graph at URI <$uri> could not be downloaded, (exception ${f.getLocalizedMessage}, ${f.getClass} cause ${f.getCause}).")
                f match {
                  //case ex: ImplementationSettings.RDFReadException if (ex.getMessage().contains("text/html")) =>
                  case ex: Exception if (ex.getMessage().contains("text/html") ||
                      isDocument ) =>
                    /* Failure(org.apache.jena.riot.RiotException: Failed to determine the content type: (
                               URI=http://ihmia.afihm.org/programme/index.html : stream=text/html)) */

                    // TODO pass transactionsInside
                    (Success(pureHTMLwebPageAnnotateAsDocument(uri, request)), Failure(f))
                  case _ => (graphDownloaded, Success(""))
                }
              }
            }
          } else {
            logger.debug(s"retrieveURIBody: mirrorURI found: $mirrorURI")
            // TODO find in Mirror URI the relevant triples ( but currently AFAIK the graph returned by this function is not used )
            (Success(emptyGraph), Success(""))
          }
          resultWhenNothingStoredLocally

        } else { // something Stored Locally: get a chance for more recent RDF data, that will be shown next time
          val resourceStatus = Success("undefined")
          Future {
            // TODO, retrieve HTTP HEAD status
            updateLocalVersion(uri, dataset).getOrElse(graphStoredLocally)
          }
          ( Success(graphStoredLocally), resourceStatus)
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
            logger.error(s"""checkIfNothingStoredLocally: URI <$uri> : $f""")
            (0, emptyGraph)
          case Success(graphStoredLocally) => {
            (graphStoredLocally.size, graphStoredLocally)
          }
        }
      logger.debug(s"""checkIfNothingStoredLocally: TDB graph at URI <$uri> size ${sizeAndGraph._1}""")
      (sizeAndGraph._1 === 0, sizeAndGraph._2)
      //      nothingStoredAndGraph.getOrElse((false, emptyGraph))
    }

    if (transactionsInside) {
      val nothingStoredAndGraph = wrapInReadTransaction {
        doCheckIfNothingStoredLocally
      }
      if (nothingStoredAndGraph.isFailure)
        logger.warn(s"checkIfNothingStoredLocally: ${nothingStoredAndGraph}")
      nothingStoredAndGraph.getOrElse((false, emptyGraph))
    } else
      doCheckIfNothingStoredLocally
  }

  /**
   * according to stored and HTTP timestamps, download if outdated;
   * with NO transaction,
   * @return a graph with more recent RDF data or Failure
   * TODO return a couple (Try[Rdf#Graph], Try[HttpURLConnection]), to indicate network error
   */
  private def updateLocalVersion(uri: Rdf#URI, dataset: DATASET): Try[Rdf#Graph] = {
    val localTimestamp = dataset2.r { getTimestampFromDataset(uri, dataset2) }.get
    /* see http://stackoverflow.com/questions/5321876/which-one-to-use-expire-header-last-modified-header-or-etags
     * TODO: code too complex */
    localTimestamp match {
      case Success(longLocalTimestamp) => {
        logger.debug(s"updateLocalVersion: $uri local TDB Timestamp: ${new Date(longLocalTimestamp)} - $longLocalTimestamp .")
        val (noErrorLastModified, timestampFromHTTPHeader, connectionOption) = lastModified(uri.toString(), httpHeadTimeout)
        logger.debug(s"updateLocalVersion: <$uri> last Modified: ${new Date(timestampFromHTTPHeader)} - no Error: $noErrorLastModified .")

        if (isDocumentExpired(connectionOption)) {
          logger.info(s"updateLocalVersion: <$uri> was OUTDATED by Expires HTPP header field")
          return readStoreURITry(uri, uri, dataset)
        }

        if (noErrorLastModified &&
           (timestampFromHTTPHeader > longLocalTimestamp
            || longLocalTimestamp === Long.MaxValue)) {
          val graph = readStoreURITry(uri, uri, dataset)
          logger.info(
            s"updateLocalVersion: <$uri> was OUTDATED by timestamp ${new Date(timestampFromHTTPHeader)}; downloaded.")

          // TODO pass arg. timestampFromHTTPHeader to addTimestampToDataset
          addTimestampToDataset(uri, dataset2) // PENDING: maybe do this in a Future

          graph

        } else if (!noErrorLastModified ||
          timestampFromHTTPHeader === Long.MaxValue) {
          connectionOption match {
            case Success(connection) =>
              val etag = getHeaderField("ETag", connection)
              val etagFromDataset = dataset2.r { getETagFromDataset(uri, dataset2) }.get
              if (etag  =/=  etagFromDataset) {
                val graph = readStoreURITry(uri, uri, dataset)
                logger.debug(s"""updateLocalVersion: <$uri> was OUTDATED by ETag; downloaded.
                  etag "$etag"  =/=  etagFromDataset "$etagFromDataset" """)
                rdfStore.rw(dataset2, { addETagToDatasetNoTransaction(uri, etag, dataset2) })
                graph
              } else Success(emptyGraph)
            case Failure(f) =>
              readStoreURITry(uri, uri, dataset)
          }
        } else Success(emptyGraph)
      }
      case Failure(fail) =>
        logger.debug(s"updateLocalVersion: <$uri> had no local Timestamp ($fail); download it:")
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
    logger.debug("After RDFCacheAlgo.storeURI " + uri + " size: " + graphFromURI.size)
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
    logger.debug(s"readStoreURINoTransaction: Before appendToGraph graphUri <$graphUri>")
    if (graphTry.isSuccess) rdfStore.appendToGraph(dataset, graphUri, graphTry.get)
    else
      logger.debug(s"storeURINoTransaction: $graphTry")
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
    if( fromUri(uri) . startsWith("file:"))
      readURIWithJenaRdfLoader( uri, dataset, request)
    else
      readURIsf( uri, dataset, request)
  }


  /** for downloading RDF uses Jena RDFDataMgr,
   *  and TODO activate Microdata Loading
   * @return  */
  private def readURIWithJenaRdfLoader(
      uri: Rdf#URI,
      dataset: DATASET,
      request: HTTPrequest ): (Try[Rdf#Graph], String) = {

    Logger.getRootLogger().info(s"readURIWithJenaRdfLoader: Before load uri $uri")

    if (isDownloadableURI(uri)) {
      // To avoid troubles with Jena cf https://issues.apache.org/jira/browse/JENA-1335
      val contentType = getContentTypeFromHEADRequest(fromUri(uri))
    	logger.debug(s""">>>> readURIWithJenaRdfLoader: getContentTypeFromHEADRequest: contentType for <$uri> "$contentType" """)
      contentType match {
        case Success(typ) =>
    	// if ( contentType.isSuccess ) {
        setTimeoutsFromConfig()
        // NOTE: Jena RDF loader can throw an exception "Failed to determine the content type"
        val graphTryLoadedFromURL = rdfLoader.load(new java.net.URL(withoutFragment(uri).toString()))
//        logger.info
        logger.debug(s"readURIWithJenaRdfLoader: after rdfLoader.load($uri): graphTryLoadedFromURL $graphTryLoadedFromURL")

        graphTryLoadedFromURL match {

          case Success(gr) =>
            (graphTryLoadedFromURL, typ)

          case Failure(f) =>
            logger.debug(s""">>>> readURIWithJenaRdfLoader: Failed with Jena RDF loader for <$uri>
               trying read With explicit content Type; ContentType From HEAD Request "$contentType" """)
            // after Java-RDFa is updated to latest Jena
            val graphFromMicrodata = graphTryLoadedFromURL.getOrElse {
              if (activateMicrodataLoading) microdataLoading(uri) else emptyGraph }

//            if( contentType  =/=  "ERROR" ) {
            /* NOTE: hoping that Jena > 3.4.0 will solve all issues on RDFDataMgr,
             * but before that , we try this */
            val gr =
              if(graphFromMicrodata.size == 0)
                readWithContentType( uri, "ERROR", dataset): Try[Rdf#Graph]
              else
                Success(graphFromMicrodata)
            logger.debug(s"""readURIWithJenaRdfLoader After readWithContentType: ${gr}""")
            ( Failure(f), "ERROR")
        }
        case Failure(f) => 
//      } else {
//        (Success(emptyGraph), contentType)
        (Failure(f), "ERROR")
      }
    } else {
      val message = s"readURIWithJenaRdfLoader: Load uri <$uri> is not possible, not a downloadable URI."
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
   * - load a file:// URI
   */
  private def readURIsf(
      uri: Rdf#URI,
      dataset: DATASET,
      request: HTTPrequest ): (Try[Rdf#Graph], String) = {

    Logger.getRootLogger().info(s"Before load uri $uri")

    if (isDownloadableURI(uri)) {
      // To avoid troubles with Jena cf https://issues.apache.org/jira/browse/JENA-1335
      val contentType = getContentTypeFromHEADRequest(fromUri(withoutFragment(uri)))

      //    	logger.debug(
      logger.debug(  "LOADING " +
    	    s""">>>> readURI: getContentTypeFromHEADRequest: contentType for <$uri> "$contentType" """)
      contentType match {
        case Success(typ) =>
          if (!typ.startsWith("text/html")) {
//          if (!contentType.startsWith("text/html") &&
//          !contentType.startsWith("ERROR") ) {
            logger.debug(s""">>>> readURIsf: for <$uri>
               trying read With explicit content Type; ContentType From HEAD Request "$contentType" """)
            val gr = readWithContentType( uri, typ, dataset): Try[Rdf#Graph]
            logger.debug(s"""readURIsf After readWithContentType: ${gr}""")
            ( gr, typ)
          } else {
            ( Success(emptyGraph), typ)
          }
        case Failure(f) =>
//      } else {
//        (Success(emptyGraph), contentType)
          ( Failure(f), "ERROR")
      }
    } else {
      val message = s"Load uri <$uri> is not possible, not a downloadable URI."
      logger.warn(message)
      ( Success(emptyGraph), "" ) // TODO return Failure( new Exception("") )
    }
  }

  /** pasted from Apache HTTP client doc
   *  https://hc.apache.org/httpcomponents-client-ga/
   * See also lastModified(), which uses plain Java library
   * */
  def getContentTypeFromHEADRequest(url0: String): Try[String] = {
    val url = url0 // TODO: test more: fromUri(withoutFragment(URI(url0)))
    val requestConfig = RequestConfig.custom().setConnectTimeout(defaultConnectTimeout).build();
    val httpclient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
    try {
      val httpHead = new HttpHead(url)
      // TODO somehow reuse trait RDFContentNegociation
      httpHead.setHeader(
          "Accept",
          "application/rdf+xml, text/turtle; charset=utf-8, application/ld+json; charset=utf-8")

      logger.debug("Executing request " + httpHead.getRequestLine());
      // Create a custom response handler
      val responseHandler = new ResponseHandler[String]() {
        override
        def handleResponse(response: HttpResponse):String = {
          val status = response.getStatusLine().getStatusCode();
          if (
              (status >= 200 && status < 300)
              // NOTE: why did I do this ???
              // || status === 406 /* Not Acceptable */ || status === 404 
              ) {
            val ct = response.getFirstHeader("Content-Type")
            if ( ct == null ) "" else ct.getValue
          } else {
            logger.error(s"---- ${response.getStatusLine()} - ${response.getAllHeaders()}");
            throw new ClientProtocolException(
                s"getContentTypeFromHEADRequest: Unexpected HTTP response status: '$status' on <$url>");
          }
        }
      };
      val responseHandled =
        if (!url.startsWith("file:")) // NOTE: necessary for deductions.runtime.jena.TestJenaHelpers
          httpclient.execute(httpHead, responseHandler)
        else
          ""
      logger.debug(s"getContentTypeFromHEADRequest: response Handled '$responseHandled'");
      Success(responseHandled)
    }
    catch {
      case t: Throwable =>
        logger.error(s"getContentTypeFromHEADRequest($url): execute: => ${t} '${t.getLocalizedMessage}'")
        Failure(t)
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
    // TODO ? accept URI's differing only on http versus https ??
    if (!fromUri(uri).startsWith(request.absoluteURL())) {
      logger.debug(  """LOADING getLocallyManagedUrlAndData
      <$uri> """ + request.absoluteURL() )
      // TODO use isFocusURIlocal()
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
          logger.debug(s"""pureHTMLwebPageAnnotateAsDocument: saved $newGraphWithUrl
            in graph <${makeAbsoluteURIForSaving(request.userId())}>""")
          newGraphWithUrl
        } else emptyGraph
      addRDFSLabelValue(uri, Some(graphURI))
      addedGraph
    }
    val currentPageTriplesIterator = wrapInReadTransaction {
      find(allNamedGraph, uri, ANY, ANY)
    }.getOrElse(Iterator.empty).toIterable
    logger.debug(s"""pureHTMLwebPageAnnotateAsDocument: addedGraphTry $addedGraphTry""")
    val result = addedGraphTry.getOrElse(emptyGraph). // newGraphWithUrl.
      // NOTE: after user added triples, this way typeChange will not be triggered
      union(makeGraph(currentPageTriplesIterator))
    logger.debug(s"pureHTMLwebPageAnnotateAsDocument: ret $result")
    result
  }
}
