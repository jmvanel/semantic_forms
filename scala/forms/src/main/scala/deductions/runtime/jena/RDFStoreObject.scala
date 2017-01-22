package deductions.runtime.jena

import java.io.File
import java.nio.file.Paths
import scala.collection.JavaConversions.asScalaIterator
import org.apache.jena.riot.RiotException
import org.apache.log4j.Logger
import org.w3.banana.jena.Jena
import org.w3.banana.jena.JenaDatasetStore
import org.w3.banana.jena.JenaModule
import org.apache.jena.graph.{ Graph => JenaGraph, Node => JenaNode, Triple => JenaTriple, _ }
import org.apache.jena.query.{ QuerySolution, ResultSet, Query => JenaQuery }
import org.apache.jena.query.DatasetFactory
import org.apache.jena.tdb.TDBFactory
import org.apache.jena.tdb.transaction.TransactionManager
import org.apache.jena.update.UpdateRequest
import deductions.runtime.dataset.RDFStoreLocalProvider
import deductions.runtime.jena.lucene.LuceneIndex
import deductions.runtime.services.Configuration
import deductions.runtime.utils.Timer

// TODO rename RDFStoreLocalJenaProvider

/**
 * singleton for implementation settings
 */
object ImplementationSettings {
  // pave the way for migration to Jena 3 ( or BlazeGraph )
  type DATASET = org.apache.jena.query.Dataset
  type Rdf = Jena
  type RDFModule = JenaModule
  /** actually just RDF database location; TODO rename RDFDatabase */
  type RDFCache = RDFStoreLocalJena1Provider
  type RDFReadException = RiotException
}

/** For user data and RDF cache, sets a default location for the Jena TDB store directory : TDB */
trait RDFStoreLocalJena1Provider
  extends RDFStoreLocalJenaProvider

/**
 * NOTES:
 * - mandatory that JenaModule (RDFModule) is first; otherwise ops may be null
 */
trait RDFStoreLocalJenaProvider
    extends MicrodataLoaderModuleJena
    with ImplementationSettings.RDFModule
    with RDFStoreLocalProvider[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with Timer
    with LuceneIndex {

  // CURRENTLY unused, but could be:  val config: Configuration
  import config._

  import ops._
  type DATASET = ImplementationSettings.DATASET
  /** very important that defensiveCopy=false, otherwise no update happens, and a big overhead for every operation */
  override val rdfStore = new JenaDatasetStore(false)

  /**
   * default is 10; each chunk commitedAwaitingFlush can be several Mb,
   *  so this can easily make an OOM exception
   */
  TransactionManager.QueueBatchSize = 5
  //  override TransactionManager.DEBUG = true

  /**
   * create (or re-connect to) TDB Database in given directory;
   *  if it is empty, create an in-memory Database
   */
  override def createDatabase(database_location: String, useTextQuery: Boolean = useTextQuery) = {
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
      //      Logger.getRootLogger.info
      println(s"RDFStoreLocalJena1Provider $database_location, dataset created: $dts")

      try {
        configureLuceneIndex(dts, useTextQuery)
      } catch {
        case t: Throwable =>
          println(t.getLocalizedMessage)
          println("getCause " + t.getCause)
          dts
      }
    } else
      DatasetFactory.createTxnMem()
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
