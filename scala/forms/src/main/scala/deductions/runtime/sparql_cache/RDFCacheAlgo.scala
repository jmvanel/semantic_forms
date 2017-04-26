package deductions.runtime.sparql_cache

import java.util.Date

import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import org.apache.log4j.Logger
import org.w3.banana.OWLPrefix
import org.w3.banana.RDF
import org.w3.banana.XSDPrefix
import org.w3.banana.io.RDFLoader
import org.w3.banana.io.RDFReader
import org.w3.banana.io.RDFXML
import org.w3.banana.io.Turtle

import deductions.runtime.dataset.RDFStoreLocalProvider
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.jena.MicrodataLoaderModule
import deductions.runtime.services.BrowsableGraph
import deductions.runtime.services.Configuration
import deductions.runtime.services.SPARQLHelpers
import deductions.runtime.utils.URIManagement
import deductions.runtime.utils.HTTPHelpers
import deductions.runtime.utils.HTTPrequest
import deductions.runtime.utils.RDFHelpers

/** */
trait RDFCacheDependencies[Rdf <: RDF, DATASET] {
  val config: Configuration
  implicit val turtleReader: RDFReader[Rdf, Try, Turtle]
  implicit val rdfXMLReader: RDFReader[Rdf, Try, RDFXML]
  implicit val rdfLoader: RDFLoader[Rdf, Try]
}

/** */
trait RDFCacheAlgo[Rdf <: RDF, DATASET] extends RDFStoreLocalProvider[Rdf, DATASET]
    with RDFCacheDependencies[Rdf, DATASET]
    with MicrodataLoaderModule[Rdf]
    with SPARQLHelpers[Rdf, DATASET]
    with TimestampManagement[Rdf, DATASET]
    with MirrorManagement[Rdf, DATASET]
    with BrowsableGraph[Rdf, DATASET]
    with RDFHelpers[Rdf]
    with URIManagement
    with HTTPHelpers {

	import scala.concurrent.ExecutionContext.Implicits.global
   
	val activateMicrodataLoading = false

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
    rdfStore.r( dataset, {
      for (graph <- rdfStore.getGraph( dataset, uri)) yield {
        val uriGraphIsEmpty = graph.size == 0
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
      retrieveURIBody(uri, dataset, HTTPrequest(), transactionsInside=true)


  /**
   * retrieve URI from a graph named by the URI itself;
   * or download and store URI, only if corresponding graph is empty,
   * or local timestamp is older;
   * timestamp is saved in another Dataset
   *  @return the more recent RDF data if any, or the old data
   */
  def retrieveURIBody(uri: Rdf#URI, dataset: DATASET,
                               request: HTTPrequest,
                               transactionsInside: Boolean
                               ): Try[Rdf#Graph] = {

    val tryGraphLocallyManagedData = getLocallyManagedUrlAndData(uri, request, transactionsInside: Boolean)

    tryGraphLocallyManagedData match {
      case Some(tgr) => Success(tgr)

      case None =>
        val tryGraphFromRdfStore = rdfStore.getGraph(dataset, uri)
        tryGraphFromRdfStore match {
          case Failure(f) => Failure(f)
          case Success(graphStoredLocally) => {
            val graphSize = if (transactionsInside)
              wrapInReadTransaction {
                graphStoredLocally.size
              }
            else
              graphStoredLocally.size
          val nothingStoredLocally = graphSize match {
                case Success(i) => i == 0
                case Failure(f) =>
                  System.err.println(s"retrieveURINoTransaction: $f")
                  true
                case i: Int => i == 0
          }
          println(s"""retrieveURINoTransaction: TDB graph at URI <$uri> size $graphSize""")

          val vvv = if (nothingStoredLocally) { // then read unconditionally from URI and store in TDB
        	  println(s"""retrieveURINoTransaction: stored Graph Is Empty for URI <$uri>""")
            val mirrorURI = getMirrorURI(uri)
                val vv = if (mirrorURI == "") {
                  val graphDownloaded = {
                      val graphTry = readURI(uri, uri, dataset, request)
                      if(transactionsInside)
                        storeURI(graphTry, uri, uri, dataset)
                      else
                        storeURINoTransaction(graphTry, uri, uri, dataset)
                    }
                  val vv = if (graphDownloaded.isSuccess) {
                    println(s"""Graph at URI <$uri>, size ${graphDownloaded.get.size}
                    Either new addition was downloaded, or locally managed data""")
                    addTimestampToDataset(uri, dataset2)
                  } else
                    println(s"Download Graph at URI <$uri> was tried, but it's faulty: $graphDownloaded")

                  println(s"""retrieveURINoTransaction: downloaded graph from URI <$uri> $graphDownloaded
                    size ${if (graphDownloaded.isSuccess) graphDownloaded.get.size}""")
                  graphDownloaded match {
                    case Success(gr) => Success(gr)
                    case Failure(f) => {
                        println(s"Graph at URI <$uri> could not be downloaded, (exception ${f.getLocalizedMessage}, ${f.getClass} cause ${f.getCause}).")
                        f match {
                          case ex: ImplementationSettings.RDFReadException if (ex.getMessage().contains("text/html")) =>
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
                  val res = // wrapInTransaction {
                    updateLocalVersion(uri, dataset).getOrElse(graphStoredLocally)
                }
                Success(graphStoredLocally)
              }
          vvv
        }
        }
    }
  }

  /**
   * according to stored and HTTP timestamps, download if outdated;
   * with NO transaction,
   * @return a graph with more recent RDF data or None
   */
  private def updateLocalVersion(uri: Rdf#URI, dataset: DATASET)
  : Try[Rdf#Graph] = {
    val localTimestamp = dataset2.r { getTimestampFromDataset(uri, dataset2) }.get
    /* see http://stackoverflow.com/questions/5321876/which-one-to-use-expire-header-last-modified-header-or-etags
     * TODO: code too complex */
    localTimestamp match {
      case Success(longLocalTimestamp) => {
        println(s"updateLocalVersion: $uri local TDB Timestamp: ${new Date(longLocalTimestamp)} - $longLocalTimestamp .")
        val ( noError, timestamp, connectionOption ) = lastModified(uri.toString(), httpHeadTimeout)
        println(s"updateLocalVersion: <$uri> last Modified: ${new Date(timestamp)} - no Error: $noError .")

        if( isDocumentExpired( connectionOption ) ) {
          println(s"updateLocalVersion: <$uri> was outdated by Expires HTPP header field")
          return readStoreURITry(uri, uri, dataset)
        }

        if ( noError && ( timestamp > longLocalTimestamp
            || longLocalTimestamp == Long.MaxValue) ) {
            val graph = readStoreURITry(uri, uri, dataset)
            println(s"updateLocalVersion: <$uri> was outdated by timestamp; downloaded.")
            addTimestampToDataset(uri, dataset2)  // PENDING: maybe do this in a Future
            graph
//          } else Success(emptyGraph) // ????

        } else if (! noError ||
          timestamp == Long.MaxValue) {
          connectionOption match {
            case Some(connection) =>
              val etag = getHeaderField("ETag", connection)
              val etagFromDataset = dataset2.r { getETagFromDataset(uri, dataset2) }.get
              if (etag != etagFromDataset) {
                val graph = readStoreURITry(uri, uri, dataset)
                println(s"""updateLocalVersion: <$uri> was outdated by ETag; downloaded.
                  etag "$etag" != etagFromDataset "$etagFromDataset" """)
                rdfStore.rw( dataset2, { addETagToDatasetNoTransaction(uri, etag, dataset2) })
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

  /** store given URI in self graph; also store imported Ontologies by owl:imports
   *  with transaction */
  private def readStoreURIinOwnGraph(uri: Rdf#URI): Rdf#Graph = {
    val graphFromURI = readStoreURI(uri, uri, dataset)
    println("RDFCacheAlgo.storeURI " + uri + " size: " + graphFromURI.size)
    wrapInTransaction {
      val it = find(graphFromURI, ANY, owl.imports, ANY)
      for (importedOntology <- it) {
        try {
          logger.info(s"Before Loading imported Ontology $importedOntology")
          foldNode(importedOntology.subject)(ontoMain => Some(ontoMain), _ => None, _ => None) match {
            case Some( uri /* : Rdf#URI */ ) =>
              foldNode(importedOntology.objectt)(onto => readStoreURINoTransaction(onto, onto, dataset),
                _ => emptyGraph,
                _ => emptyGraph
                ) ; Unit
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
    val r = rdfStore.rw( dataset, {
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
    val graphTry = readURI(uri, graphUri, dataset, request)
    storeURINoTransaction(graphTry, uri, graphUri, dataset)
  }
   /** read unconditionally from URI and store in TDB, Transaction inside */
  private def readStoreURITry(uri: Rdf#URI, graphUri: Rdf#URI, dataset: DATASET,
                              request: HTTPrequest = HTTPrequest()): Try[Rdf#Graph] = {
    val graphTry = readURI(uri, graphUri, dataset, request)
    storeURI(graphTry, uri, graphUri, dataset)
  }
  
  private def storeURINoTransaction(
    graphTry: Try[Rdf#Graph], uri: Rdf#URI, graphUri: Rdf#URI, dataset: DATASET): Try[Rdf#Graph] = {
    logger.info(s"readStoreURINoTransaction: Before appendToGraph uri <$uri> graphUri <$graphUri>")
    if (graphTry.isSuccess) rdfStore.appendToGraph(dataset, graphUri, graphTry.get)
    // TODO: reuse appendToGraph return
    logger.info(s"readStoreURINoTransaction: uri <$uri> : stored into graphUri <$graphUri>")
    graphTry
  }

  /** store URI in TDB, including Transaction */
  private def storeURI(
    graphTry: Try[Rdf#Graph], uri: Rdf#URI, graphUri: Rdf#URI, dataset: DATASET): Try[Rdf#Graph] = {
    wrapInTransaction {
      storeURINoTransaction(graphTry, uri, graphUri, dataset)
    }.flatten
  }

	/**
   * read unconditionally from URI,
   * no matter what the concrete syntax is;
   * can also load an URI with the # part
   */
  private def readURI(uri: Rdf#URI, graphUri: Rdf#URI, dataset: DATASET,
      request:HTTPrequest = HTTPrequest()): Try[Rdf#Graph] = {
    Logger.getRootLogger().info(s"Before load uri $uri into graphUri $graphUri")

    if (isDownloadableURI(uri)) {
      setTimeoutsFromConfig()
      // NOTE: Jena RDF loader can throw an exception "Failed to determine the content type"
      val graphTry = rdfLoader.load(new java.net.URL(uri.toString()))
      logger.info(s"readStoreURINoTransaction: after rdfLoader.load($uri): $graphTry")

      // TODO
//      val graph = graphTry.getOrElse {
//        if(activateMicrodataLoading)
//          microdataLoading(uri)
//        else
//          emptyGraph
//      }
      graphTry
    } else {
      val message = s"Load uri <$uri> is not possible, not a downloadable URI."
      logger.warn(message)
      Success(emptyGraph)  // TODO return Failure( new Exception("") )
    }
  }
  
  private def microdataLoading(uri: Rdf#URI): Rdf#Graph = {
    logger.info(s"Trying RDFa for <$uri>")
    microdataLoader.load(
      new java.net.URL(uri.toString())) match {
        case Success(s) => s
        case Failure(f) => {

          logger.error("readStoreURINoTransaction: START MESSAGE")
          logger.error(f.getMessage)
          logger.error(s""" uri.toString.contains("/ldp/") ${uri.toString.contains("/ldp/")} """)
          logger.error("END MESSAGE")

          // catch only "pure" HTML web page: TODO? make a function isPureHTMLwebPage(uri: URI, request: Request): Boolean
          //              if (f.getMessage.contains("Failed to determine the content type:")) {
          //                logger.info(s"<$uri> is a pure HTML web page (no RDFa or microformats");
          //                val tryGraph = getLocallyManagedUrlAndData(uri, request)
          //                tryGraph . get
          //              } else
          throw f
        }
      }
  }

  /** test if it's a locally managed URL, that is created locally and 100% located here */
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

  /** needs Write transaction */
  def pureHTMLwebPageAnnotateAsDocument(uri: Rdf#URI, request: HTTPrequest): Rdf#Graph = {
    val newGraphWithUrl = makeGraph(List(makeTriple(uri, rdf.typ, foaf.Document)))
    wrapInTransaction {
      rdfStore.appendToGraph(
        dataset,
        URI(makeAbsoluteURIForSaving(request.userId())),
        newGraphWithUrl)
    }
    println(s"pureHTMLwebPageAnnotateAsDocument: saved $newGraphWithUrl in graph <${makeAbsoluteURIForSaving(request.userId())}>")
    val currentPageTriplesIterator = wrapInReadTransaction {
      find(allNamedGraph, uri, ANY, ANY)
    }.getOrElse(Iterator.empty) . toIterable
    val result = newGraphWithUrl.
      // NOTE: after user added triples, this way typeChange will not be triggered
      union(makeGraph(currentPageTriplesIterator))
    println(s"pureHTMLwebPageAnnotateAsDocument: ret $result")
    result
  }
}

