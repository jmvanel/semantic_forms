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
import deductions.runtime.services.SPARQLHelpers
import deductions.runtime.utils.RDFHelpers

//import deductions.runtime.abstract_syntax.InstanceLabelsInferenceMemory0


/** */
trait RDFCacheDependencies[Rdf <: RDF, DATASET] {
  implicit val turtleReader: RDFReader[Rdf, Try, Turtle]
  implicit val rdfXMLReader: RDFReader[Rdf, Try, RDFXML]
  implicit val rdfLoader: RDFLoader[Rdf, Try]
}

/** */
trait RDFCacheAlgo[Rdf <: RDF, DATASET] extends RDFStoreLocalProvider[Rdf, DATASET]
    with RDFCacheDependencies[Rdf, DATASET]
//    with RDFLoader[Rdf, Try]
    with SPARQLHelpers[Rdf, DATASET]
    with TimestampManagement[Rdf, DATASET]
    with MirrorManagement[Rdf, DATASET]
    with BrowsableGraph[Rdf, DATASET]
    with RDFHelpers[Rdf] {

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
    dataset.r({
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
    dataset.rw({
      retrieveURINoTransaction(uri: Rdf#URI, dataset: DATASET)
    }).flatMap { identity }
  }

  /**
   * retrieve URI from a graph named by the URI itself;
   * or download and store URI only if corresponding graph is empty,
   * or local timestamp is older;
   * timestamp is saved in another Dataset
   */
  def retrieveURINoTransaction(uri: Rdf#URI, dataset: DATASET): Try[Rdf#Graph] = {
    for (graph <- rdfStore.getGraph(dataset, uri)) yield {
      val uriGraphIsEmpty = graph.size == 0
      println(s"retrieveURINoTransaction.uriGraphIsEmpty: $uriGraphIsEmpty <$uri>")
      if (uriGraphIsEmpty) {
        val mirrorURI = getMirrorURI(uri)
        if (mirrorURI == "") {
          try {
            val g = storeURINoTransaction(uri, uri, dataset)
            if (g.size > 0) {
              println("Graph at URI was downloaded, new addition: " + uri + " , size " + g.size)
              addTimestampToDataset(uri, dataset2)
            } else
              println(s"Graph at URI <$uri> was downloaded, but it's empty.")
            g
          } catch {
            case t: Exception =>
              println(s"Graph at URI $uri could not be downloaded, trying local TDB (${t.getLocalizedMessage}).")
              val tryGraph = search_only(fromUri(uri))
              tryGraph match {
                case Success(g)   => g
                case Failure(err) => throw err
              }
          }
        } else {
          println(s"mirrorURI found: $mirrorURI")
          // TODO find in Mirror URI the relevant triples ( but currently AFAIK the graph returned by this function is not used )
          emptyGraph
        }
      } else {
        updateLocalVersion(uri, dataset)
        graph
      }
    }
  }

  /**
   * according to timestamp download if outdated;
   * with NO transaction
   */
  def updateLocalVersion(uri: Rdf#URI, dataset: DATASET) = {
//      dataset.rw({
        val localTimestamp = dataset2.r{
          getTimestampFromDataset(uri, dataset2)
        } . get
        localTimestamp match {
          case Success(longLocalTimestamp) => {
            println(s"updateLocalVersion: $uri local TDB Timestamp: ${new Date(longLocalTimestamp) } - $longLocalTimestamp .")
            val lastModifiedTuple = lastModified(uri.toString(), httpHeadTimeout)
            println(s"$uri last Modified: ${new Date(lastModifiedTuple._2)} - $lastModifiedTuple .")
            
            if (lastModifiedTuple._1) {
            	if (lastModifiedTuple._2 > longLocalTimestamp
            	    || longLocalTimestamp == Long.MaxValue ) {
            		storeURINoTransaction(uri, uri, dataset)
            		println(s"$uri was outdated by timestamp; downloaded.")
//            		addTimestampToDatasetNoTransaction(uri, dataset)
            		addTimestampToDataset(uri, dataset2)
            	}
            } else if (! lastModifiedTuple._1 ||
                lastModifiedTuple._2 == Long.MaxValue ) {
              lastModifiedTuple._3 match {
                case Some(connection) => 
                val etag = headerField( fromUri(uri), "ETag": String, connection )
                val etagFromDataset = dataset2.r{ getETagFromDataset(uri, dataset2) } .get
                if(etag != etagFromDataset) {
                	storeURINoTransaction(uri, uri, dataset)
                  println(s"$uri was outdated by ETag; downloaded.")
                  dataset2.rw{ addETagToDatasetNoTransaction(uri, etag, dataset2) }
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
//      })
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

  /** store given URI in self graph; also store imported Ontologies by owl:imports
   *  NOTE: the dataset is provided by the parent trait;
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
   * read from uri and store in TDB, no matter what the syntax is;
   * can also load an URI with the # part
   */
  def storeURINoTransaction(uri: Rdf#URI, graphUri: Rdf#URI, dataset: DATASET): Rdf#Graph = {
    Logger.getRootLogger().info(s"Before load uri $uri into graphUri $graphUri")

    if (isDownloadableURI(uri)) {
      System.setProperty("sun.net.client.defaultReadTimeout", defaultReadTimeout.toString)
      System.setProperty("sun.net.client.defaultConnectTimeout", defaultConnectTimeout.toString)
      val graph: Rdf#Graph =
        rdfLoader.load(new java.net.URL(uri.toString())).get
      Logger.getRootLogger().info(s"Before storeURI uri $uri graphUri $graphUri")
      rdfStore.appendToGraph( dataset, graphUri, graph)
      Logger.getRootLogger().info(s"storeURI uri $uri : stored into graphUri $graphUri")
      graph

    } else {
      val message = s"Load uri $uri is not possible, not a downloadable URI."
      Logger.getRootLogger().warn(message)
      emptyGraph
    }
  }

}

