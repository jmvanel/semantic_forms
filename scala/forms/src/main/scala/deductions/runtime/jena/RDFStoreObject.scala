package deductions.runtime.jena

import scala.collection.JavaConversions.asScalaIterator
import org.apache.log4j.Logger
import org.w3.banana.jena.Jena
import org.w3.banana.jena.JenaModule
import com.hp.hpl.jena.query.Dataset
import com.hp.hpl.jena.tdb.TDBFactory
import deductions.runtime.dataset.RDFStoreLocalProvider
import org.w3.banana.jena.JenaDatasetStore
import org.w3.banana._
import org.w3.banana.diesel._
import deductions.runtime.utils.Timer

/** singleton  hosting a Jena TDB database in directory "TDB" */
object RDFStoreObject extends JenaModule with RDFStoreLocalJena1Provider

/** For user data and RDF cache, sets a default location for the Jena TDB store directory : TDB */
trait RDFStoreLocalJena1Provider extends RDFStoreLocalJenaProvider {
}

trait RDFStoreLocalJenaProvider extends RDFStoreLocalProvider[Jena, Dataset]
    with JenaModule with JenaRDFLoader
    with Timer {
  import ops._
  type DATASET = Dataset
  override val rdfStore = new JenaDatasetStore(false)
  import rdfStore.graphStoreSyntax._

  override def createDatabase(database_location: String) = {
    val dts = TDBFactory.createDataset(database_location)
    Logger.getRootLogger.info(s"RDFStoreLocalJena1Provider $database_location, dataset created: $dts")
    dts
  }

  /**
   * NOTES:
   *  - no need of a transaction here, as getting Union Graph is anyway part of a transaction
   *  - Union Graph in Jena should be re-done for each use (not 100% sure, but safer anyway)
   */
  override def allNamedGraph: Rdf#Graph = {
    time(s"allNamedGraph dataset $dataset", {
      val ang = dataset.getGraph(makeUri("urn:x-arq:UnionGraph")).get
      println(s"Union Graph: hashCode ${ang.hashCode()} : size ${ang.size}")
      ang
    }
    )
    //    union(dataset.getDefaultModel.getGraph :: unionGraph :: Nil)
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
