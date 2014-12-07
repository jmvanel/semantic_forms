package deductions.runtime.sparql_cache
import deductions.runtime.jena.RDFStoreObject

object TestRDFCache extends App with RDFCacheJena {
  val uri = "http://jmvanel.free.fr/jmv.rdf#me"

  import Ops._
//  lazy val store = RDFStoreObject.store
  retrieveURI(makeUri(uri), store)

}