package deductions.runtime.sparql_cache
import deductions.runtime.jena.RDFStoreObject
import org.scalatest.FunSuite
import deductions.runtime.utils.FileUtils
import org.w3.banana.SparqlOpsModule

class TestRDFCache extends FunSuite with RDFCache
    with SparqlOpsModule {
  val uri = "http://jmvanel.free.fr/jmv.rdf#me"
  //  src/test/resources/foaf.n3
  import ops._
  import sparqlOps._

  FileUtils.deleteLocalSPARL

  test("save to enpoint cache and check with SPARQL that endpoint is populated.") {
    val uriNode = makeUri(uri)
    val g = retrieveURI(uriNode, dataset)
    println("graph from " + uri + " size " + g.get.size)
    val r = rdfStore.rw(RDFStoreObject.dataset, {
      val g = rdfStore.getGraph(dataset, uriNode)
      g
    })
    println("rdfStore.getGraph( dataset, uriNode).get " + g.get)

    val queryString = s"""
      # PREFIX foaf:
      CONSTRUCT {
        <$uri> ?P ?V .
        <$uri> <is:in> ?G .
      }
      WHERE {
        GRAPH ?G { 
          <$uri> ?P ?V .
        }
      }
    """
    rdfStore.r(RDFStoreObject.dataset, {
      val result = for {
        query <- parseConstruct(queryString)
        es <- rdfStore.executeConstruct(RDFStoreObject.dataset, query, Map())
      } yield es.size()

      val r = result.get
      println("size " + r)
      assert(r > 0) // TODO
    })
  }
}
