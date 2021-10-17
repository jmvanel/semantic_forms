package deductions.runtime.sparql_cache

import java.util.Date

import deductions.runtime.utils.RDFStoreLocalProvider
import deductions.runtime.utils.{HTTPHelpers, RDFHelpers, URIManagement}
import deductions.runtime.core.HTTPrequest
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpHead
import org.apache.http.client.{ClientProtocolException, ResponseHandler}
import org.apache.http.impl.client.HttpClients
// import org.apache.log4j.Logger
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

  implicit def rdfLoader(): RDFLoader[Rdf, Try]

  implicit val turtleWriter: RDFWriter[Rdf, Try, Turtle]
  implicit val jsonldCompactedWriter: RDFWriter[Rdf, Try, JsonLdCompacted]
  implicit val rdfXMLWriter: RDFWriter[Rdf, Try, RDFXML]
}

/** */
trait RDFCacheAlgo[Rdf <: RDF, DATASET]
extends RDFCacheDependencies[Rdf, DATASET]
    with MicrodataLoaderModule[Rdf]
    with TimestampManagement[Rdf, DATASET]
    with MirrorManagement[Rdf, DATASET]
    with BrowsableGraph[Rdf, DATASET]
    with RDFHelpers[Rdf]
    with URIManagement
    with HTTPHelpers
    with TypeAddition[Rdf, DATASET]
    with StringHelpers
    with CSVadaptor[Rdf, DATASET]
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
  def retrieveURIResourceStatus(uriArg: Rdf#URI, dataset: DATASET,
                      request: HTTPrequest,
                      transactionsInside: Boolean): (Try[Rdf#Graph], Try[String]) = {

    val uriNoFrag = withoutFragment(uriArg)
    val uri = findDefinitionURI(uriNoFrag)

    val tryGraphLocalData = getLocallyManagedUrlAndData(uri, request, transactionsInside)
    logger.debug( "retrieveURIResourceStatus: CONSIDERING " + s"<$uriArg> tryGraphLocallyManagedData $tryGraphLocalData")

    tryGraphLocalData match {
      case Some(tgr) => (Success(tgr), Success(""))
      case None =>

        val (nothingStoredLocally, graphStoredLocally) = checkIfNothingStoredLocally(uri, transactionsInside)

        val result: (Try[Rdf#Graph], Try[String]) =
          if (nothingStoredLocally) { // then read unconditionally from URI and store in TDB
          logger.debug(s"retrieveURIResourceStatus: stored Graph Is Empty for URI <$uri> => read URI <$uri>")
          val mirrorURI = getMirrorURI(uri)
          val resultWhenNothingStoredLocally: (Try[Rdf#Graph], Try[String]) =
            if (mirrorURI === "") {
            val graphTry_MIME: (Try[Rdf#Graph], String) = readURI(uri, dataset, request)
            logger.debug(  "LOADING in TDB " + s""">>>> retrieveURIResourceStatus graphTry_MIME $graphTry_MIME""")
            val graphDownloaded: Try[Rdf#Graph] = {
              val graphTry = graphTry_MIME._1
              if (transactionsInside)
                storeURI(graphTry, uri, dataset)
              else
                storeURINoTransaction(graphTry, uri, dataset)
            }
            if (graphDownloaded.isSuccess) {
              logger.debug(  "LOADING isSuccess " +
                  s"""Graph at URI <$uri>, size ${graphDownloaded.get.size}
                    Either new addition was downloaded, or locally managed data""")
              if( ! request.isFocusURIlocal() )
                addTimestampToDataset(uri, dataset2)
            } else
              logger.error(s"Download Graph at URI <$uri> was tried, but it's faulty: $graphDownloaded")

            val contentType = graphTry_MIME._2
            logger.debug(s"""retrieveURIResourceStatus: downloaded graph from URI <$uri> $graphDownloaded
                    size ${if (graphDownloaded.isSuccess) graphDownloaded.get.size} content Type: $contentType""")
            val isDocument = isDocumentMIME(contentType) // TODO case RDFa
            graphDownloaded match {
              case Success(gr) =>
                if (!isDocument)
                  (Success(gr), Success(""))
                else
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
            logger.debug(s"retrieveURIResourceStatus: mirrorURI found: $mirrorURI")
            // TODO find in Mirror URI the relevant triples ( but currently AFAIK the graph returned by this function is not used )
            (Success(emptyGraph), Success(""))
          }
          resultWhenNothingStoredLocally

        } else { // something Stored Locally: get a chance for more recent RDF data, that will be shown next time
          val resourceStatus = Success("undefined")
          Future { // TODO, retrieve HTTP HEAD status
            updateLocalVersion(uri, dataset).getOrElse(graphStoredLocally)
          }
          ( Success(graphStoredLocally), resourceStatus)
        }
        result
    }
  }

  /** find Definition URI by detecting rdfs:isDefinedBy triple;
   *  e.g. for foaf: vocab' */
  private def findDefinitionURI(uri: Rdf#URI): Rdf#URI = {
    val node =
      wrapInReadTransaction{
        val x = find( allNamedGraph, uri, rdfs.isDefinedBy , ANY) . toList . headOption . getOrElse(
         Triple(nullURI, nullURI, uri)) . objectt
       logger.debug(s"findDefinitionURI: x '$x'")
       x
    } . getOrElse(uri)
    val ret = foldNode(node)(u=>u, bn=>uri, lit=>uri)
    logger.info(s"findDefinitionURI: ret '$ret'")
    ret
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
    /* see http://stackoverflow.com/questions/5321876/which-one-to-use-expire-header-last-modified-header-or-etags
     * TODO: code too complex
     * maybe have 3 functions like:
     * def isOutdatedByLastModified(uri: Rdf#URI, connectionOption: Try[HttpURLConnection], localTimestamp): Try[Long]:
     *   (Boolean, String) */
    val localTimestamp = dataset2.r { getTimestampFromDataset(uri, dataset2) }.get
    val currentTimestamp = (new Date).getTime
    localTimestamp match {
      case Success(longLocalTimestamp) => {
        logger.debug(s"updateLocalVersion: <$uri> local TDB Timestamp: ${new Date(longLocalTimestamp)} - $longLocalTimestamp .")
        val (noErrorLastModified, timestampFromHTTPHeader, connectionOption) =
          lastModified(uri.toString(), httpHeadTimeout)
        logger.debug(s"updateLocalVersion: <$uri> last Modified: ${new Date(timestampFromHTTPHeader)} - no Error: $noErrorLastModified .")

        val (outdatedByExpires, expiresTimestamp) = isDocumentExpired(connectionOption)
        // if Expired is older than LastModified, do not take it in account
        if ( outdatedByExpires &&
            expiresTimestamp > timestampFromHTTPHeader &&
            expiresTimestamp > currentTimestamp  &&
            expiresTimestamp > longLocalTimestamp) {
          logger.info(s"updateLocalVersion: <$uri> was OUTDATED by Expires HTPP header field")
          return readStoreURITry(uri, uri, dataset, request=HTTPrequest() )
        }

        if (noErrorLastModified &&
           // if LastModified is in the future, do not take it in account
            timestampFromHTTPHeader < currentTimestamp &&
           (timestampFromHTTPHeader > longLocalTimestamp
             || longLocalTimestamp === Long.MaxValue)) {
          val graph = readStoreURITry(uri, uri, dataset, request=HTTPrequest() )
          logger.info(
            s"updateLocalVersion: <$uri> was OUTDATED by timestamp ${new Date(timestampFromHTTPHeader)}; downloaded.")

          // TODO pass arg. timestampFromHTTPHeader to addTimestampToDataset
          addTimestampToDataset(uri, dataset2)

          return graph

        } else if (!noErrorLastModified ||
          // indicates LastModified not present:
          timestampFromHTTPHeader === Long.MaxValue) {
          logger.debug(
            s"updateLocalVersion: <$uri> timestamp COULD NOT BE CHECKED : ${new Date(timestampFromHTTPHeader)}")
          connectionOption match {
            case Success(connection) =>
              val etag = getHeaderField("ETag", connection)
              val etagFromDataset = dataset2.r { getETagFromDataset(uri, dataset2) }.get
              if (etag  =/=  etagFromDataset) {
                logger.info(s"""updateLocalVersion: <$uri> was OUTDATED by ETag; downloading...""")
                val graph = readStoreURITry(uri, uri, dataset, request=HTTPrequest())
                logger.debug(s"""updateLocalVersion: <$uri> was OUTDATED by ETag; downloaded.
                  etag "$etag"  =/=  etagFromDataset "$etagFromDataset" """)
                rdfStore.rw(dataset2, { addETagToDatasetNoTransaction(uri, etag, dataset2) })
                graph
              } else {
                logger.info(s"""updateLocalVersion: <$uri> seems UP TO DATE.""")
                Success(emptyGraph)
              }
            case Failure(f) =>
              logger.warn(s"updateLocalVersion: <$uri> Failure for ETag ($f); download <$uri>")
              readStoreURITry(uri, uri, dataset, request=HTTPrequest())
          }
        } else {
          logger.info(s"""updateLocalVersion: <$uri> all tests pased, considered UP TO DATE.""")
          Success(emptyGraph)
        }
      }
      case Failure(fail) =>
        logger.info(s"updateLocalVersion: <$uri> had no local Timestamp ($fail); download it:")
        readStoreURITry(uri, uri, dataset, request=HTTPrequest() )
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
    val graphFromURI = readStoreURI(uri, uri, dataset)
    logger.debug("After RDFCacheAlgo.storeURI " + uri + " size: " + graphFromURI.size)
    wrapInTransaction {
      val it = find(graphFromURI, ANY, owl.imports, ANY)
      for (importedOntology <- it) {
        try {
          logger.info(s"Before Loading imported Ontology $importedOntology")
          foldNode(importedOntology.subject)(ontoMain => Some(ontoMain), _ => None, _ => None) match {
            case Some(uri /* : Rdf#URI */ ) =>
              foldNode(importedOntology.objectt)(onto => readStoreURINoTransaction(onto, onto, dataset,
                  request=HTTPrequest() ),
                _ => emptyGraph,
                _ => emptyGraph); ()
            case None => ()
          }
        } catch {
          case e: Throwable => logger.error(e.getLocalizedMessage)
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
      readStoreURINoTransaction(uri, graphUri, dataset, request=HTTPrequest() )
    })
    r.flatten match {
      case Success(g) => g
      case Failure(e) =>
        logger.error("ERROR in readStoreURI: " + e)
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
                                        request: HTTPrequest ): Try[Rdf#Graph] = {
    val graphTry = readURI(uri, dataset, request) . _1
    storeURINoTransaction(graphTry, graphUri, dataset)
  }
  /** read unconditionally from URI and store in TDB, Transaction inside */
  private def readStoreURITry(uri: Rdf#URI, graphUri: Rdf#URI, dataset: DATASET,
                              request: HTTPrequest ): Try[Rdf#Graph] = {
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

  def storeURINoTransaction(
    graphTry: Try[Rdf#Graph], graphUri: Rdf#URI, dataset: DATASET): Try[Rdf#Graph] = {
    logger.debug(s"storeURINoTransaction: Before appendToGraph graphUri <$graphUri>")
    if (graphTry.isSuccess) {
      val uriWithoutFragment = withoutFragment(graphUri)
      rdfStore.appendToGraph(dataset, uriWithoutFragment, graphTry.get)
      logger.info(s"storeURINoTransaction: stored into graphUri <$uriWithoutFragment>")
    } else
      logger.warn(s"storeURINoTransaction: graphUri <$graphUri> : Try: $graphTry")
    // TODO ? reuse appendToGraph
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

    logger.info(s"readURIWithJenaRdfLoader: Before load uri <$uri>")

    if (isDownloadableURI(uri)) {
      // To avoid troubles with Jena cf https://issues.apache.org/jira/browse/JENA-1335
      val contentType = getContentTypeFromHEADRequest(fromUri(uri))
    	logger.debug(s""">>>> readURIWithJenaRdfLoader: getContentTypeFromHEADRequest: contentType for <$uri> "$contentType" """)
      contentType match {
        case Success(typ) =>
        setTimeoutsFromConfig()
        // NOTE: Jena RDF loader can throw an exception "Failed to determine the content type"
        val graphTryLoadedFromURL = rdfLoader.load(new java.net.URL(withoutFragment(uri).toString()))
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
        case Failure(f) => (Failure(f), "ERROR")
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
   * an URI with # fragment is loaded into an graph named without the fragment
   * TODO:
   * an ontology URI without fragment is loaded into an graph named after the ontology
   */
  private def readURIsf(
      uri: Rdf#URI,
      dataset: DATASET,
      request: HTTPrequest ): (Try[Rdf#Graph], String) = {

    logger.info(s"readURIsf: Before load uri <$uri>")

    if (isDownloadableURI(uri)) {
      // To avoid troubles with Jena cf https://issues.apache.org/jira/browse/JENA-1335
      val uriWithoutFragment = fromUri(withoutFragment(uri))
      val contentType = getContentTypeFromHEADRequest(uriWithoutFragment)
      logger.debug(  "LOADING " +
        s""">>>> readURIsf: getContentTypeFromHEADRequest: contentType for <$uri> "$contentType" """)
      contentType match {
        case Success(typ) =>
          logger.info(s"readURIsf: MIME type: '$typ'")
          if( ! typ.startsWith("text/html")) {
            if(
              typ == "text/csv" ||
              typ == "text/comma-separated-values") {
                ( readCSVfromURL(uri, typ, dataset, request), typ )

          } else {
            logger.debug(s""">>>> readURIsf: for <$uri>
               trying read With explicit content Type; ContentType From HEAD Request "$contentType" """)
            val gr = readWithContentType( uri, typ, dataset): Try[Rdf#Graph]
            logger.debug(s"""readURIsf After readWithContentType: ${gr}""")
            ( gr, typ)
          }
          } else {
            ( Success(emptyGraph), typ)
          }
        case Failure(f) => ( Failure(f), "ERROR getContentTypeFromHEADRequest")
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
    val requestConfig = RequestConfig.custom().
      setConnectTimeout(defaultConnectTimeout).
      setSocketTimeout(defaultReadTimeout).
      build();
    val httpclient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
    try {
      val httpHead = new HttpHead(url)
      // TODO somehow reuse trait RDFContentNegociation
      httpHead.setHeader(
          "Accept",
          "application/rdf+xml, text/turtle; charset=utf-8, application/ld+json; charset=utf-8")

          // For GEoNature ...
          //      httpHead.setHeader(
//    		  "Cookie",
//    		  "token=eyJhbGciOiJIUzI1NiIsImlhdCI6MTU5NjcwMjM4MywiZXhwIjoxNTk3MzA3MTgzfQ.eyJpZF9yb2xlIjoxLCJub21fcm9sZSI6IkFkbWluaXN0cmF0ZXVyIiwicHJlbm9tX3JvbGUiOiJ0ZXN0IiwiaWRfYXBwbGljYXRpb24iOjMsImlkX29yZ2FuaXNtZSI6LTEsImlkZW50aWZpYW50IjoiYWRtaW4iLCJpZF9kcm9pdF9tYXgiOjF9.4mbwrP9iQ8gTHewWLvJMhl1NiFHnP2C_UQgiMBrni0o; session=.eJyrVvJ3dg5xjFCyqlYqLU4tik8uKi1LTQFxnZWslIyVdJRcoLQrlA6C0qFQOgxM19bWAgAPNhKx.Eg1atA.kvCblUrgeP-yZJ8RyZaS5eNf_PY")

      logger.debug("Executing request " + httpHead.getRequestLine());
      // Create a custom response handler
      val responseHandler = new ResponseHandler[String]() {
        override
        def handleResponse(response: HttpResponse):String = {
          val status = response.getStatusLine().getStatusCode();
          if (
              (status >= 200 && status < 300)
                /* NOTE: why doing ? when the original URL is not RDF, but somehow the database has some content
                   e.g. reading an RDF document with this URL as subject */
               || status === 406 /* Not Acceptable */ || status === 404
              ) {
            val ct = response.getFirstHeader("Content-Type")
            if ( ct == null ) "" else ct.getValue
          } else {
            logger.error(s"---- ${response.getStatusLine()} - ${response.getAllHeaders()}");
            throw new ClientProtocolException(
                s"getContentTypeFromHEADRequest: Unexpected HTTP response status: '${response.getStatusLine()}' on <$url>");
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

  /** get Local data in any graph from given URI : <URI> ?P ?O
   *  @param
   *  transactionsInside: need transaction inside this function */
  private def getLocallyManagedUrlAndData(uri: Rdf#Node, request: HTTPrequest, transactionsInside: Boolean): Option[Rdf#Graph] =
    // TODO bad smell in code: remove ! in test
    if (! request.isFocusURIlocal() ) {
      logger.debug(  s"""getLocallyManagedUrlAndData: CONSIDERING <$uri> from <${request.absoluteURL()}>""" )
      // then it can be a "pure" HTML web page, or an RDF document
      None
    } else {
      logger.debug( s"""getLocallyManagedUrlAndData: locally managed user URL""" )

      // it's a locally managed user URL and data, no need to download anything
      // used by formatHTMLStatistics():
      // TODO test rather: Some(rdfStore.getGraph( dataset, uri ).getOrElse(emptyGraph))

      val gr1 =
        if (transactionsInside)
          findAllNamedGraphUriANY_ANYTransaction(uri)
        else
          Success( findAllNamedGraphUriANY_ANY(uri) )

      gr1 match {
        case Success(gr) => Some(gr)
        case Failure(f) =>
          logger.error(s"getLocallyManagedUrlAndData: URI $uri - $f")
          None
      }
    }

  /** find
   *  Uri ANY ANY
   *  in All Named Graphs ;
   *  Needs wrapInReadTransaction */
  def findAllNamedGraphUriANY_ANY(uri: Rdf#Node) : Rdf#Graph =
    makeGraph(find(allNamedGraph, uri, ANY, ANY).toIterable)

  /** find
   *  Uri ANY ANY
   *  in All Named Graphs */
  def findAllNamedGraphUriANY_ANYTransaction(uri: Rdf#Node) : Try[Rdf#Graph] =
    wrapInReadTransaction { findAllNamedGraphUriANY_ANY(uri) }

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
