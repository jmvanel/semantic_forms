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

//trait RDFStore2[Rdf <: RDF] extends RDFStore[Rdf,  Try, Dataset]
//with Transactor[Rdf, Dataset]
//with SparqlUpdate[Rdf,  Try, Dataset]

/** singleton  hosting a Jena TDB database in directory "TDB" */
object RDFStoreObject extends RDFStoreLocalProvider
with JenaModule {
  // TODO remove allNamedGraphs elsewhere
  lazy val allNamedGraphs = rdfStore.getGraph(dataset, ops.makeUri("urn:x-arq:UnionGraph"))
	import MonadicHelpers._
  lazy val allNamedGraphsFuture = tryToFuture( allNamedGraphs )
}

trait RDFStoreLocalProvider {
  type DATASET = Dataset
  lazy val dataset : DATASET = TDBFactory.createDataset("TDB")
}

trait RDFGraphPrinter extends RDFStoreLocalProvider with JenaModule {
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