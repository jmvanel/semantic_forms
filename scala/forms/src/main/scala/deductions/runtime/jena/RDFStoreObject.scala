package deductions.runtime.jena

import org.w3.banana.jena.JenaStore
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

// transition to Banana 0.7.1 ( interim )

trait RDFStore2[Rdf <: RDF] extends RDFStore[Rdf] with Transactor[Rdf, Dataset] with SparqlUpdate
trait JenaModule2 extends JenaModule {
    implicit val rdfStore: RDFStore2[Jena] = new JenaDatasetStore(true)
    with RDFStore2[Jena]
}

/** singleton  hosting a Jena TDB database in directory "TDB" */
object RDFStoreObject extends RDFStoreLocalProvider

trait RDFStoreLocalProvider
extends JenaModule2 {
  type DATASET = Dataset
  lazy val dataset : DATASET = TDBFactory.createDataset("TDB")

  // TODO remove !!!!!! not existing in Banana 0.7
//  lazy val store : RDFStore[Jena] = JenaStore(dataset, defensiveCopy = false)
  lazy val store = JenaStore(dataset, defensiveCopy = false)

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