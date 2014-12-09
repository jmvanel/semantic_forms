package deductions.runtime.sparql_cache
import deductions.runtime.jena.RDFStoreObject

object TestRDFCache extends App with RDFCache {
  val uri = "http://jmvanel.free.fr/jmv.rdf#me"

  import ops._
//  lazy val store = RDFStoreObject.store
  retrieveURI( makeUri(uri), dataset)

}