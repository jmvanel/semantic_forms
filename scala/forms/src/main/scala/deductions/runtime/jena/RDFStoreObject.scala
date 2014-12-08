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

// transition to Banana 0.7.1 ( interim )

trait RDFStore2[Rdf <: RDF] extends RDFStore[Rdf,  Try, Dataset]
with Transactor[Rdf, Dataset]
with SparqlUpdate[Rdf,  Try, Dataset]
//trait JenaModule2 extends JenaModule {
//    implicit val rdfStore: RDFStore2[Jena] = new JenaDatasetStore(true)
//    with RDFStore2[Jena]
//}

/** singleton  hosting a Jena TDB database in directory "TDB" */
object RDFStoreObject extends RDFStoreLocalProvider
with JenaModule

trait RDFStoreLocalProvider
//extends JenaModule
{
  type DATASET = Dataset
  lazy val dataset : DATASET = TDBFactory.createDataset("TDB")

  //  removed !!!!!! not existing in Banana 0.7
//  lazy val store : RDFStore[Jena] = JenaStore(dataset, defensiveCopy = false)
//  lazy val store = JenaStore(dataset, defensiveCopy = false)
}

trait RDFGraphPrinter extends RDFStoreLocalProvider with JenaModule
{
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