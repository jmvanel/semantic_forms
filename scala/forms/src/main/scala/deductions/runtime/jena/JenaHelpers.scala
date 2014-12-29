package deductions.runtime.jena

import org.w3.banana.jena.Jena
import org.apache.log4j.Logger
import org.apache.jena.riot.RDFDataMgr
import org.w3.banana.RDFOpsModule
import org.w3.banana.SparqlOps
import java.net.URL
import org.w3.banana.jena.JenaModule
import org.w3.banana.RDFStore
import org.w3.banana.diesel._
import org.w3.banana.RDFStore
import org.w3.banana.RDFOpsModule
import org.w3.banana.RDFOps
import scala.util.Try
import org.w3.banana.RDF
import org.w3.banana.jena.io.JenaRDFReader
import scala.util.Success
import scala.util.Failure

/**
 * Helpers for RDF Store
 *  In Banana 0.7 :
 *  - JenaStore is not existing anymore
 *  - generic API for transactions
 *
 * TODO rename RDFStoreHelpers
 * TODO remove dependency to Jena after anyRDFReader is introduced in Banana
 */

trait JenaHelpers extends JenaModule {
  import ops._

  type Store = RDFStoreObject.DATASET

  /**
   * store URI in a named graph, with transaction,
   * using Jena's RDFDataMgr
   * (use content-type or else file extension)
   * with Jena Riot for smart reading of any format,
   * cf https://github.com/w3c/banana-rdf/issues/105
   */
  def storeURI(uri: Rdf#URI, graphUri: Rdf#URI, dataset: Store): Rdf#Graph = {
    val r = rdfStore.rw(dataset, {
      storeURINoTransaction(uri, graphUri, dataset)
    })
    r match {
      case Success(g) => g
      case Failure(e) =>
        Logger.getRootLogger().error("ERROR: " + e)
        throw e
    }
  }

  def storeURINoTransaction(uri: Rdf#URI, graphUri: Rdf#URI, dataset: Store): Rdf#Graph = {
    Logger.getRootLogger().info(s"storeURI uri $uri graphUri $graphUri")
    val gForStore = rdfStore.getGraph(dataset, graphUri)
    // read from uri no matter what the syntax is:
    val graph = RDFDataMgr.loadModel(uri.toString()).getGraph
    //        val graph = anyRDFReader.readAnyRDFSyntax(uri.toString()) . get // TODO
    rdfStore.appendToGraph(dataset, uri, graph)
    Logger.getRootLogger().info(s"storeURI uri $uri : stored")
    graph
    //      }
  }

}
