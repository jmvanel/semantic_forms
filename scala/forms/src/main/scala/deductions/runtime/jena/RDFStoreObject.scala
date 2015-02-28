package deductions.runtime.jena

import scala.collection.JavaConversions.asScalaIterator
import org.apache.log4j.Logger
import org.w3.banana.jena.Jena
import org.w3.banana.jena.JenaModule
import com.hp.hpl.jena.query.Dataset
import com.hp.hpl.jena.tdb.TDBFactory
import deductions.runtime.dataset.RDFStoreLocalProvider
import org.w3.banana.jena.JenaDatasetStore

/** singleton  hosting a Jena TDB database in directory "TDB" */
object RDFStoreObject extends JenaModule with RDFStoreLocalJena1Provider {
}

/** For user data and RDF cache, sets a default location for the Jena TDB store directory : ./TDB/ */
trait RDFStoreLocalJena1Provider extends RDFStoreLocalJenaProvider {
  override lazy val dataset: DATASET = TDBFactory.createDataset("TDB")
}

/** For application data (timestamps, URI types, ...), sets a default location for the Jena TDB store directory : ./TDBapp/ */
trait RDFStoreLocalJena2Provider extends RDFStoreLocalJenaProvider {
  override lazy val dataset: DATASET = TDBFactory.createDataset("TDBapp")
}

trait RDFStoreLocalJenaProvider extends RDFStoreLocalProvider[Jena, Dataset] with JenaModule {
  //  override 
  type DATASET = Dataset
  override val rdfStore = new JenaDatasetStore(false)
  import rdfStore.graphStoreSyntax._
  /**
   * NOTES:
   *  - no need of a transaction here, as getting Union Graph is anyway part of a transaction
   *  - Union Graph in Jena should be re-done for each use (not 100% sure, but safer anyway)
   */
  override def allNamedGraph: Rdf#Graph = {
    dataset.getGraph(ops.makeUri("urn:x-arq:UnionGraph")).get
  }
}

/** TODO implement independently of Jena */
trait RDFGraphPrinter extends RDFStoreLocalJena1Provider {
  import rdfStore.transactorSyntax._
  def printGraphList {
    dataset.r({
      val lgn = dataset.asDatasetGraph().listGraphNodes()
      Logger.getRootLogger().info(s"listGraphNodes size ${lgn.size}")
      for (gn <- lgn) {
        Logger.getRootLogger().info(s"${gn.toString()}")
      }
      Logger.getRootLogger().info(s"afer listGraphNodes")
    })
  }
}