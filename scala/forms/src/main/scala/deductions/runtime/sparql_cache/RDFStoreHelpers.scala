package deductions.runtime.sparql_cache

import org.apache.log4j.Logger
import java.net.URL
import org.w3.banana.diesel._
import org.w3.banana.RDF
import org.w3.banana.io.RDFLoader
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import deductions.runtime.dataset.RDFStoreLocalProvider

/**
 * Helpers for RDF Store
 *
 */

trait RDFStoreHelpers[Rdf <: RDF, DATASET] extends RDFStoreLocalProvider[Rdf, DATASET]
    with RDFLoader[Rdf, Try] {

  import ops._
  import rdfStore.transactorSyntax._
  import rdfStore.graphStoreSyntax._

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
   *  probably can also load an URI with the # part ???
   */
  def storeURINoTransaction(uri: Rdf#URI, graphUri: Rdf#URI, dataset: DATASET): Rdf#Graph = {
    Logger.getRootLogger().info(s"Before storeURI uri $uri graphUri $graphUri")
    //    val gForStore = dataset.getGraph(graphUri)
    Logger.getRootLogger().info(s"Before load uri $uri into graphUri $graphUri")
    val graph: Rdf#Graph =
      load(new java.net.URL(uri.toString())).get.
        asInstanceOf[Rdf#Graph]
    dataset.appendToGraph(graphUri, graph)
    Logger.getRootLogger().info(s"storeURI uri $uri : stored into graphUri $graphUri")
    graph
  }

}
