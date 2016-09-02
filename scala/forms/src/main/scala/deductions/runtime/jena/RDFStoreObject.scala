package deductions.runtime.jena

import scala.collection.JavaConversions.asScalaIterator
import org.apache.log4j.Logger
import org.w3.banana.jena.Jena
import org.w3.banana.jena.JenaDatasetStore
import org.w3.banana.jena.JenaModule
import com.hp.hpl.jena.tdb.TDBFactory
import com.hp.hpl.jena.tdb.transaction.TransactionManager
import deductions.runtime.dataset.RDFStoreLocalProvider
import deductions.runtime.services.DefaultConfiguration
import deductions.runtime.utils.Timer
import deductions.runtime.services.Configuration
import com.hp.hpl.jena.rdf.model.ModelFactory
import com.hp.hpl.jena.query.DatasetFactory
import deductions.runtime.jena.lucene.LuceneIndex
import java.io.File
import java.nio.file.Paths

// TODO rename RDFStoreLocalJenaProvider

/** singleton for implementation settings */
object ImplementationSettings {
  // pave the way for migration to Jena 3 ( or BlazeGraph )
  type DATASET = com.hp.hpl.jena.query.Dataset
  type Rdf = Jena
}

/** For user data and RDF cache, sets a default location for the Jena TDB store directory : TDB */
trait RDFStoreLocalJena1Provider
  extends RDFStoreLocalJenaProvider
  with DefaultConfiguration

trait RDFStoreLocalJenaProvider
    extends RDFStoreLocalProvider[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with JenaModule // with JenaRDFLoader
    with Timer
    with Configuration
    with LuceneIndex {
  import ops._
  type DATASET = ImplementationSettings.DATASET
  /** very important that defensiveCopy=false, otherwise no update happens, and a big overhead for every operation */
  override val rdfStore = new JenaDatasetStore(false)
  import rdfStore.graphStoreSyntax._

  /**
   * default is 10; each chunk commitedAwaitingFlush can be several Mb,
   *  so this can easily make an OOM exception
   */
  TransactionManager.QueueBatchSize = 5
  //  override TransactionManager.DEBUG = true

  /**
   * create TDB Database in given directory;
   *  if it is empty, create an in-memory Database
   */
  override def createDatabase(database_location: String) = {
    if (database_location != "") {

      // if the directory does not exist, create it
      val currentRelativePath = Paths.get("");
      val abs = currentRelativePath.toAbsolutePath().toString();
      System.out.println("Current relative path is: " + abs);
      val dir = new File(abs, database_location)
      if (!dir.exists()) {
        System.out.println("creating database directory: " +
          database_location + " as " + dir + " - current (.) : " + new File(".").getAbsolutePath);
        dir.mkdirs()
      }

      val dts = TDBFactory.createDataset(Paths.get(database_location).toString())
      Logger.getRootLogger.info(s"RDFStoreLocalJena1Provider $database_location, dataset created: $dts")

      try {
        configureLuceneIndex(dts)
      } catch {
        case t: Throwable =>
          println(t.getLocalizedMessage)
          println("getCause " + t.getCause)
          dts
      }
    } else
      DatasetFactory.createMem()
  }

  /**
   * NOTES:
   *  - no need of a transaction here, as getting Union Graph is anyway part of a transaction
   *  - Union Graph in Jena should be re-done for each use (not 100% sure, but safer anyway)
   */
  override def allNamedGraph: Rdf#Graph = {
    time(s"allNamedGraph dataset $dataset", {
      //      println(s"Union Graph: entering for $dataset")

      // NOTE: very important to use the properly configured rdfStore (with defensiveCopy=false)
      val ang = rdfStore.getGraph(dataset, makeUri("urn:x-arq:UnionGraph")).get
      //      println(s"Union Graph: hashCode ${ang.hashCode()} : size ${ang.size}")
      ang
    })
    //    union(dataset.getDefaultModel.getGraph :: unionGraph :: Nil)
  }

  /** List the names of graphs */
  def listNames(ds: DATASET): Iterator[String] = ds.listNames()

  /** make an MGraph from a Dataset */
  def makeMGraph(graphURI: Rdf#URI, ds: DATASET = dataset): Rdf#MGraph = {
    println(s"makeMGraph( $graphURI")
    val nm = ds.getNamedModel(fromUri(graphURI))
    nm.getGraph
  }

  def close(ds: DATASET) = ds.close()
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
