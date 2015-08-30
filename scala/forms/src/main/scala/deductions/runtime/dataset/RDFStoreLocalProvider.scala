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

/**
 * abstract RDFStore Local Provider
 */
trait RDFStoreLocalProvider[Rdf <: RDF, DATASET] {

  /** relative or absolute file path for the database */
  val databaseLocation: String
  def createDatabase(database_location: String = databaseLocation): DATASET
  //  override ?? 
  lazy val dataset: DATASET = createDatabase(databaseLocation)

  /** NOTE: same design pattern as for XXXModule in Banana */
  implicit val rdfStore: RDFStore[Rdf, Try, DATASET] with SparqlUpdate[Rdf, Try, DATASET]

  implicit val ops: RDFOps[Rdf]
  implicit val sparqlOps: SparqlOps[Rdf]
  implicit val sparqlGraph: SparqlEngine[Rdf, Try, Rdf#Graph]

  def allNamedGraph: Rdf#Graph
}

trait RDFStoreLocalUserManagement[Rdf <: RDF, DATASET] extends RDFStoreLocalProvider[Rdf, DATASET] {
  import ops._
  val passwordsGraph = rdfStore.getGraph(dataset, URI("urn:users")).get
}
