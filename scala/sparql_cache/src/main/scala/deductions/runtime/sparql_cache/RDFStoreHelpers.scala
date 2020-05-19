package deductions.runtime.sparql_cache

import deductions.runtime.utils.RDFStoreLocalProvider
import deductions.runtime.utils.Configuration
import org.w3.banana.RDF
import org.w3.banana.io.RDFLoader

import scala.util.{Failure, Success, Try}

/**
 * Helpers for RDF Store
 *
 */

trait RDFStoreHelpers[Rdf <: RDF, DATASET] extends RDFStoreLocalProvider[Rdf, DATASET] {

  val config: Configuration
  implicit val rdfLoader: RDFLoader[Rdf, Try]

  import config._
  import ops. _

  /**
   * store URI in a named graph,
   * transactional,
   * using Jena's RDFDataMgr
   * with Jena Riot for smart reading of any format,
   * (use content-type or else file extension)
   * cf https://github.com/w3c/banana-rdf/issues/105
   */
  def storeURI(uri: Rdf#URI, graphUri: Rdf#URI, dataset: DATASET): Rdf#Graph = {
    val r = rdfStore.rw( dataset, {
      storeURINoTransaction(uri, graphUri, dataset)
    })
    r match {
      case Success(g) => g
      case Failure(e) =>
        logger.error("ERROR: " + e)
        throw e
    }
  }

  /**
   * read from uri no matter what the syntax is;
   *  probably can also load an URI with the # part ???
   */
  def storeURINoTransaction(uri: Rdf#URI, graphUri: Rdf#URI, dataset: DATASET): Rdf#Graph = {
    logger.info(s"Before load uri $uri into graphUri $graphUri")
    System.setProperty("sun.net.client.defaultReadTimeout", defaultReadTimeout.toString)
    System.setProperty("sun.net.client.defaultConnectTimeout", defaultConnectTimeout.toString)

    val graph: Rdf#Graph =
      /* TODO check new versions of Scala > 2.11.6 that this asInstanceOf is 
        still necessary */
      rdfLoader.load(new java.net.URL( withoutFragment(uri).toString())).get.
        asInstanceOf[Rdf#Graph]
    rdfStore.appendToGraph( dataset, graphUri, graph)
    logger.info(s"storeURI uri $uri : stored into graphUri $graphUri")
    graph
  }

}
