package deductions.runtime.utils

import org.w3.banana.{RDF, RDFOps, RDFStore, SparqlEngine, SparqlOps, SparqlUpdate}

import scala.util.Try

import scalaz._
import Scalaz._

/** RDF OPerations on a DataBase */
trait RDFOPerationsDB[Rdf <: RDF, DATASET] {
  val config: Configuration
//  import config._
  
  /** NOTE: same design pattern as for XXXModule in Banana */
  implicit val rdfStore: RDFStore[Rdf, Try, DATASET] with SparqlUpdate[Rdf, Try, DATASET]
  implicit val ops: RDFOps[Rdf]
  implicit val sparqlOps: SparqlOps[Rdf]
  implicit val sparqlGraph: SparqlEngine[Rdf, Try, Rdf#Graph]  
}

/**
 * abstract RDFStore Local Provider
 */
trait RDFStoreLocalProvider[Rdf <: RDF, DATASET]
extends RDFOPerationsDB[Rdf, DATASET] {

//  CURRENTLY unused, but could be:  val config: Configuration

  /** relative or absolute file path for the RDF Jena database 
   *  TODO put in Configuration */
  val databaseLocation: String = "TDB"

  /** create (or re-connect to) TDB Database in given directory */
  def createDatabase(database_location: String = databaseLocation,
      useTextQuery: Boolean= config.useTextQuery,
      useSpatialIndex: Boolean= config.useSpatialIndex
      ): DATASET

  val dataset: DATASET // = createDatabase(databaseLocation)

  def allNamedGraph: Rdf#Graph
  
  /** For application data (timestamps, URI types, ...),
   *  sets a default location for the Jena TDB store directory : TDB2/ */
  val databaseLocation2 = "TDB2"
  val databaseLocation3 = "TDB3"
  lazy val dataset2: DATASET = createDatabase(databaseLocation2, false, false)
  lazy val dataset3: DATASET = createDatabase(databaseLocation3, false, false)
  // TODO datasetForLabels
  
  /** List the names of graphs */
  def listNames(ds: DATASET = dataset): Iterator[String]
  
  /** make an MGraph from a Dataset */
  def makeMGraph( graphURI: Rdf#URI, ds: DATASET = dataset): Rdf#MGraph

  def close(ds: DATASET = dataset)

  def readWithContentType( uri: Rdf#URI, contentType: String, dataset: DATASET): Try[Rdf#Graph]
  
  def datasetSize() = rdfStore.rw( dataset, { datasetSizeNoTR() })
  def datasetSizeNoTR() = ops.graphSize(allNamedGraph)

  // implementations

    /** */
  def getDatasetOrDefault(databaseLoc: String = "TDB", useTextQuery: Boolean= config.useTextQuery): DATASET = {
    if (databaseLoc === databaseLocation)
      dataset
    else
      createDatabase(databaseLoc, useTextQuery)
  }


  import scala.collection.JavaConverters._

  /** TODO move this to Banana */
  def listGraphNames(ds: DATASET = dataset): Seq[String] = {
    ds match {
      case ds: org.apache.jena.query.Dataset =>
        ds.listNames() . asScala . toSeq
      case _ => Seq()
    }
  }
}

trait RDFStoreLocalUserManagement[Rdf <: RDF, DATASET]
extends RDFStoreLocalProvider[Rdf, DATASET] {
  import ops._
  /**
   * NOTE:
   *  - no need of a transaction here, as getting Union Graph is anyway part of a transaction
   *  TODO : put User Management in another database
   */
  def passwordsGraph: Rdf#MGraph = {
    makeMGraph( URI("urn:users")
        , dataset3 
        )
  }
}
