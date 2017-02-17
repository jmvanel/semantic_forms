package deductions.runtime.sparql_cache

import java.util.Date

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
import deductions.runtime.services.BrowsableGraph
import deductions.runtime.services.Configuration
import deductions.runtime.services.SPARQLHelpers
import deductions.runtime.services.URIManagement
import deductions.runtime.utils.RDFHelpers
import deductions.runtime.jena.MicrodataLoaderModule
import deductions.runtime.utils.HTTPrequest

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
    with URIManagement{


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
  def retrieveURI(uri: Rdf#URI, dataset: DATASET = dataset): Try[Rdf#Graph] = {
    rdfStore.rw( dataset, {
      retrieveURINoTransaction(uri: Rdf#URI, dataset: DATASET)
    }).flatMap { identity }
  }

  /**
   * retrieve URI from a graph named by the URI itself;
   * or download and store URI only if corresponding graph is empty,
   * or local timestamp is older;
   * timestamp is saved in another Dataset
   *  @return the more recent RDF data if any, or the old data
   */
  def retrieveURINoTransaction(uri: Rdf#URI, dataset: DATASET,
      request:HTTPrequest = HTTPrequest()
  ): Try[Rdf#Graph] = {
    for (graph <- rdfStore.getGraph(dataset, uri)) yield {
      val nothingStoredLocally = graph.size == 0
      println(s"retrieveURINoTransaction: stored Graph Is Empty: $nothingStoredLocally URI <$uri>")

      if (nothingStoredLocally) { // then read unconditionally from URI and store in TDB

        val mirrorURI = getMirrorURI(uri)
        if (mirrorURI == "") {
          try {
            val g = readStoreURINoTransaction(uri, uri, dataset, request)
            if (g.size > 0) {
              println("Graph at URI was downloaded, new addition: " + uri + " , size " + g.size)
              addTimestampToDataset(uri, dataset2)
            } else
              println(s"Download Graph at URI <$uri> was tried, but it's empty.")
            g
          } catch {
            case t: Exception =>
              println(s"Graph at URI <$uri> could not be downloaded, trying local TDB (exception ${t.getLocalizedMessage}, ${t.getClass} cause ${t.getCause}).")
              val tryGraph = search_only(fromUri(uri))
              tryGraph match {
                case Success(g) if( g.size > 1 ) => g  // 1 because there is always urn:displayLabel
                case Success(_) => throw t
                case Failure(err) => throw err
              }

            case t: Throwable =>
              // for Java-RDFa release 0.4.2 :
              println(s"Graph at URI <$uri> could not be downloaded, exception: $t") ; emptyGraph
          }
        } else {
          println(s"mirrorURI found: $mirrorURI")
          // TODO find in Mirror URI the relevant triples ( but currently AFAIK the graph returned by this function is not used )
          emptyGraph
        }

      } else { // get a chance for more recent RDF data
        updateLocalVersion(uri, dataset) . getOrElse(graph)
      }
    }
  }

  /**
   * according to stored and HTTP timestamps, download if outdated;
   * with NO transaction,
   * @return a graph with more recent RDF data or None
   */
  private def updateLocalVersion(uri: Rdf#URI, dataset: DATASET)
  : Option[Rdf#Graph] = {
    val localTimestamp = dataset2.r { getTimestampFromDataset(uri, dataset2) }.get

    /* TODO:
     * - code too complex;
     * - probably code belongs TimestampManagement
     *  see http://stackoverflow.com/questions/5321876/which-one-to-use-expire-header-last-modified-header-or-etags
     */
    localTimestamp match {
      case Success(longLocalTimestamp) => {
        println(s"updateLocalVersion: $uri local TDB Timestamp: ${new Date(longLocalTimestamp)} - $longLocalTimestamp .")
        val lastModifiedTuple = lastModified(uri.toString(), httpHeadTimeout)
        println(s"updateLocalVersion: <$uri> last Modified: ${new Date(lastModifiedTuple._2)} - $lastModifiedTuple .")

        if( isDocumentExpired( connectionOption = lastModifiedTuple._3 ) ) {
          println(s"updateLocalVersion: <$uri> was outdated by Expires HTPP header field")
          return Some(readStoreURINoTransaction(uri, uri, dataset))
        }

        if (lastModifiedTuple._1) {
          if (lastModifiedTuple._2 > longLocalTimestamp
            || longLocalTimestamp == Long.MaxValue) {
            val graph = readStoreURINoTransaction(uri, uri, dataset)
            println(s"updateLocalVersion: <$uri> was outdated by timestamp; downloaded.")
            // PENDING: maybe do this in a Future
            addTimestampToDataset(uri, dataset2)
            Some(graph)
          } else None
        } else if (!lastModifiedTuple._1 ||
          lastModifiedTuple._2 == Long.MaxValue) {
          lastModifiedTuple._3 match {
            case Some(connection) =>
              val etag = getHeaderField("ETag", connection)
              val etagFromDataset = dataset2.r { getETagFromDataset(uri, dataset2) }.get
              if (etag != etagFromDataset) {
                val graph = readStoreURINoTransaction(uri, uri, dataset)
                println(s"updateLocalVersion: <$uri> was outdated by ETag; downloaded.")
                // PENDING: maybe do this in a Future
                rdfStore.rw( dataset2, { addETagToDatasetNoTransaction(uri, etag, dataset2) })
                Some(graph)
              } else None
            case None =>
              Some(readStoreURINoTransaction(uri, uri, dataset))
          }
        } else None
      }
      case Failure(fail) =>
        println(s"updateLocalVersion: <$uri> had no local Timestamp ($fail); download it:")
        Some(readStoreURINoTransaction(uri, uri, dataset))
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
   *  NOTE: the dataset is provided by the parent trait;
   *  with transaction */
  private def readStoreURIinOwnGraph(uri: Rdf#URI): Rdf#Graph = {
    val graphFromURI = readStoreURI(uri, uri, dataset)
    println("RDFCacheAlgo.storeURI " + uri + " size: " + graphFromURI.size)
    rdfStore.rw( dataset, {
      val it = find(graphFromURI, ANY, owl.imports, ANY)
      for (importedOntology <- it) {
        try {
          logger.info(s"Before Loading imported Ontology $importedOntology")
          foldNode(importedOntology.subject)(ontoMain => Some(ontoMain), _ => None, _ => None) match {
            case Some( _ ) =>
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
    })
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
    r match {
      case Success(g) => g
      case Failure(e) =>
        Logger.getRootLogger().error("ERROR: " + e)
        throw e
    }
  }
      
  /**
   * read unconditionally from URI and store in TDB, no matter what the syntax is;
   * can also load an URI with the # part
   */
  private def readStoreURINoTransaction(uri: Rdf#URI, graphUri: Rdf#URI, dataset: DATASET,
      request:HTTPrequest = HTTPrequest()): Rdf#Graph = {
    Logger.getRootLogger().info(s"Before load uri $uri into graphUri $graphUri")

    if (isDownloadableURI(uri)) {
      System.setProperty("sun.net.client.defaultReadTimeout", defaultReadTimeout.toString)
      System.setProperty("sun.net.client.defaultConnectTimeout", defaultConnectTimeout.toString)

      // NOTE: Jena RDF loader can throw an exception "Failed to determine the content type"
      val graphTry = rdfLoader.load(new java.net.URL(uri.toString()))
      logger.info(s"readStoreURINoTransaction: after rdfLoader.load($uri): $graphTry")

      val graph = graphTry.getOrElse {
        logger.info(s"Trying RDFa for <$uri>")
        microdataLoader.load(
          new java.net.URL(uri.toString())) match {
            case Success(s) => s
            case Failure(f) => {

              logger.info("readStoreURINoTransaction: START MESSAGE")
              logger.info(f.getMessage)
              logger.info(s""" uri.toString.contains("/ldp/") ${uri.toString.contains("/ldp/")} """)
              logger.info("END MESSAGE")

              // catch only "pure" HTML web page: TODO make a function isPureHTMLwebPage(uri: URI, request: Request): Boolean
              if (f.getMessage.contains("Failed to determine the content type:")) {

                /* test if it's a locally managed URL; TODO move the test to top of function;
                 * indeed the test is independent of the exception */
                if (!fromUri(uri).startsWith(request.absoluteURL(""))) {
                  // then it's really a "pure" HTML web page (and not a locally managed URL and data)
                  logger.info(s"<$uri> is a pure HTML web page (no RDFa or microformats");
                  { // TODO move this towards root in call stack, to put this triple in user graph
                    val newTripleWithURL = List(makeTriple(uri, rdf.typ, foaf.Document))
                    val newGraphWithUrl: Rdf#Graph = makeGraph(newTripleWithURL)
                    newGraphWithUrl
                  }
                  emptyGraph
                } else
                  // it's a locally managed URL and data, no need to download anything
                  emptyGraph
              } else throw f
            }
            }
              
          }
      
      logger.info(s"readStoreURINoTransaction: graph $graph")

      Logger.getRootLogger().info(s"readStoreURINoTransaction: Before appendToGraph uri <$uri> graphUri <$graphUri>")
      rdfStore.appendToGraph( dataset, graphUri, graph)
      Logger.getRootLogger().info(s"readStoreURINoTransaction: uri <$uri> : stored into graphUri <$graphUri>")
      graph

    } else {
      val message = s"Load uri <$uri> is not possible, not a downloadable URI."
      Logger.getRootLogger().warn(message)
      emptyGraph
    }
  }

}

