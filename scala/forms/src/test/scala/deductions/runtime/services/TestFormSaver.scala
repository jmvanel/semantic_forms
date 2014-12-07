package deductions.runtime.services

import java.net.URLEncoder
import org.w3.banana.RDFOpsModule
import deductions.runtime.jena.RDFStoreObject
import deductions.runtime.sparql_cache.RDFCacheJena
import org.w3.banana.SparqlGraphModule
import org.w3.banana.SparqlOpsModule
import org.w3.banana.PointedGraph
import org.w3.banana.diesel.toPointedGraphW
import org.scalatest.FunSuite

class TestFormSaver 
  extends FunSuite with RDFCacheJena // TODO depend on generic Rdf
   with RDFOpsModule
   with SparqlGraphModule
   with SparqlOpsModule
{
  import Ops._
  
//  lazy val store =  RDFStoreObject.store
  lazy val fs = new FormSaver(store)
  
  val uri = "http://jmvanel.free.fr/jmv.rdf#me"
  retrieveURI(makeUri(uri), store)
  val map: Map[String, Seq[String]] = Map(
      "uri" ->  Seq( encode(uri) ),
      "url" ->  Seq( encode(uri) ),
      encode("LIT-http://xmlns.com/foaf/0.1/familyName") -> Seq("Van Eel"),
      encode("ORIG-LIT-http://xmlns.com/foaf/0.1/familyName") -> Seq("Vanel")
  )

  test("remove the old value from TDB when saving") {
    fs.saveTriples(map)
    store.readTransaction {
      val graph = store.getGraph(makeUri("urn:x-arq:UnionGraph"))
      val pg = PointedGraph[Rdf](makeUri(uri), graph)
      val objects = pg / makeUri("http://xmlns.com/foaf/0.1/familyName")
      val familyNames = objects.map(_.pointer).toSet
      // assert that foaf:familyName "Vanel" has been removed from Triple store
      assert(familyNames.size == 1)
      assert(familyNames.contains(makeLiteral("Van Eel", xsd.string)))
    }
  }
  ////
  def encode(uri:String) = { URLEncoder.encode(uri, "utf-8" ) }
}