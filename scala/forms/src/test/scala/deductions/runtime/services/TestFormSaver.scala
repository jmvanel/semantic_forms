package deductions.runtime.services

import java.net.URLEncoder
import org.w3.banana.RDFOpsModule
import deductions.runtime.jena.RDFStoreObject
import deductions.runtime.sparql_cache.RDFCache
import org.w3.banana.SparqlGraphModule
import org.w3.banana.SparqlOpsModule
import org.w3.banana.PointedGraph
import org.w3.banana.diesel._
import org.scalatest.FunSuite
import deductions.runtime.jena.RDFStoreLocalProvider

class TestFormSaver
    extends FunSuite
    with RDFCache
    with RDFOpsModule
    with SparqlGraphModule
    with SparqlOpsModule
//    with RDFStoreLocalProvider2[Rdf, DATASET]
    with RDFStoreLocalProvider
{
  import ops._

  lazy val fs = new FormSaver()

  val uri = "http://jmvanel.free.fr/jmv.rdf#me"
  retrieveURI(makeUri(uri), dataset)
  val map: Map[String, Seq[String]] = Map(
    "uri" -> Seq(encode(uri)),
    "url" -> Seq(encode(uri)),
    encode("LIT-http://xmlns.com/foaf/0.1/familyName") -> Seq("Van Eel"),
    encode("ORIG-LIT-http://xmlns.com/foaf/0.1/familyName") -> Seq("Vanel")
  )

  test("remove the old value from TDB when saving") {
    fs.saveTriples(map)
    rdfStore.r(dataset, {
      //    store.readTransaction {
      val graph = rdfStore.getGraph(dataset, makeUri("urn:x-arq:UnionGraph"))
      val pg = PointedGraph[Rdf](makeUri(uri), graph.get)
      val objects = pg / makeUri("http://xmlns.com/foaf/0.1/familyName")
      val familyNames = objects.map(_.pointer).toSet
      // assert that foaf:familyName "Vanel" has been removed from Triple store
      assert(familyNames.size == 1)
      assert(familyNames.contains(makeLiteral("Van Eel", xsd.string)))
    })
  }
  ////
  def encode(uri: String) = { URLEncoder.encode(uri, "utf-8") }
}