package deductions.runtime.jena

import com.hp.hpl.jena.tdb.TDBFactory
import org.w3.banana.jena.JenaModule
import org.apache.log4j.Logger
import scala.collection.JavaConversions._
import org.w3.banana.RDFStore
import org.w3.banana.jena.Jena
import org.w3.banana.jena.JenaDatasetStore
import org.w3.banana.SparqlUpdate
import org.w3.banana.Transactor
import com.hp.hpl.jena.query.Dataset
import org.w3.banana.RDF
import scala.util.Try
import deductions.runtime.utils.MonadicHelpers
import org.w3.banana.RDFOpsModule
import deductions.runtime.dataset.RDFStoreLocalProvider

/** singleton  hosting a Jena TDB database in directory "TDB" */
object RDFStoreObject extends JenaModule with RDFStoreLocalJena1Provider {
  // TODO remove allNamedGraphs elsewhere
  lazy val allNamedGraphs = rdfStore.getGraph(dataset, ops.makeUri("urn:x-arq:UnionGraph"))
  import MonadicHelpers._
  lazy val allNamedGraphsFuture = tryToFuture(allNamedGraphs)
}

/** sets a default location for the Jena TDB store directory : ./TDB/ */
trait RDFStoreLocalJena1Provider extends RDFStoreLocalProvider[Jena, Dataset] with JenaModule {
  override type DATASET = Dataset
  lazy val dataset: DATASET = TDBFactory.createDataset("TDB")
}

/**
 * abstract RDFStore Local Provider
 */
//trait RDFStoreLocalProvider2[Rdf <: RDF, DATASET] extends RDFOpsModule {
//  // NOTE: same design pattern as for XXXModule in Banana
//  implicit val rdfStore: RDFStore[Rdf, Try, DATASET]
//  type DATASET
//  val dataset: DATASET
//}

/** TODO implement independently of Jena */
trait RDFGraphPrinter extends RDFStoreLocalJena1Provider {
  def printGraphList {
    rdfStore.r(dataset, {
      val lgn = dataset.asDatasetGraph().listGraphNodes()
      Logger.getRootLogger().info(s"listGraphNodes size ${lgn.size}")
      for (gn <- lgn) {
        Logger.getRootLogger().info(s"${gn.toString()}")
      }
      Logger.getRootLogger().info(s"afer listGraphNodes")
    })
  }
}