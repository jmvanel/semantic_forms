package deductions.runtime.dataset

import scala.util.Try
import org.w3.banana.RDF
import org.w3.banana.RDFOpsModule
import org.w3.banana.RDFStore
import org.w3.banana.SparqlOpsModule
import org.w3.banana.RDFOps
import org.w3.banana.SparqlOps
import org.w3.banana.SparqlEngine
import org.w3.banana.SparqlUpdate
import org.w3.banana.URIOps

trait RDFOPerationsDB[Rdf <: RDF, DATASET] {
    /** NOTE: same design pattern as for XXXModule in Banana */
  implicit val rdfStore: RDFStore[Rdf, Try, DATASET] with SparqlUpdate[Rdf, Try, DATASET]
  implicit val ops: RDFOps[Rdf]
  implicit val sparqlOps: SparqlOps[Rdf]
  implicit val sparqlGraph: SparqlEngine[Rdf, Try, Rdf#Graph]
}

/**
 * abstract RDFStore Local Provider
 */
trait RDFStoreLocalProvider[Rdf <: RDF, DATASET] extends RDFOPerationsDB[Rdf, DATASET] {

  /** relative or absolute file path for the database */
  val databaseLocation: String = "TDB"
  def createDatabase(database_location: String = databaseLocation): DATASET
  //  override ?? 
  lazy val dataset: DATASET = createDatabase(databaseLocation)

  def allNamedGraph: Rdf#Graph
  
  /** For application data (timestamps, URI types, ...),
   *  sets a default location for the Jena TDB store directory : TDB2/ */
  val databaseLocation2 = "TDB2"
  lazy val dataset2: DATASET = createDatabase(databaseLocation2)
}

trait RDFStoreLocalUserManagement[Rdf <: RDF, DATASET] extends RDFStoreLocalProvider[Rdf, DATASET] {
  import ops._
  /**
   * NOTE:
   *  - no need of a transaction here, as getting Union Graph is anyway part of a transaction
   *  TODO : want to put User Management in another database
   */
  def passwordsGraph: Rdf#Graph = {
    rdfStore.getGraph(dataset, URI("urn:users")).get
  }
}
