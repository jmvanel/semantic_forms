package deductions.runtime.sparql_cache
//import deductions.runtime.jena.RDFStoreObject
import org.scalatest.FunSuite
import deductions.runtime.utils.FileUtils
import org.w3.banana.SparqlOpsModule
import org.scalatest.Ignore
import org.scalatest.BeforeAndAfterAll
import org.w3.banana.jena.JenaModule
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import deductions.runtime.jena.RDFCache
import deductions.runtime.services.DefaultConfiguration

//@Ignore
class TestRDFCache extends FunSuite with RDFCache
    with SparqlOpsModule
    with BeforeAndAfterAll
    with JenaModule
    with RDFStoreLocalJena1Provider
    with SitesURLForDownload {

  val config = new DefaultConfiguration {
    override val useTextQuery = false
  }
  val uri = "http://jmvanel.free.fr/jmv.rdf#me"
  //  val uri2 = "http://live.dbpedia.org/page/Taraxacum_japonicum"
  //  val uri2 = uri 
  val uri2 = githubcontent + "/jmvanel/rdf-i18n/master/foaf/foaf.fr.ttl"
  //  src/test/resources/foaf.n3

  import ops._
  import sparqlOps._

  override def afterAll {
    println("afterAll: deleteLocalSPARQL")
    FileUtils.deleteLocalSPARQL()
  }

  test("save to enpoint cache and check with SPARQL that endpoint is populated.") {
    val uriNode = makeUri(uri)
    val g = retrieveURI(uriNode, dataset)
    println("graph from " + uri + " size " + g.get.size)
    val r = rdfStore.rw(dataset, {
      val g = rdfStore.getGraph(dataset, uriNode)
      g
    })
    //    println("rdfStore.getGraph( dataset, uriNode).get " + g.get)
    checkNamedGraph(uri)
  }

  test("save to enpoint cache with storeURI() and check with SPARQL.") {
    val uriNode = makeUri(uri2)
    readStoreUriInNamedGraph(uriNode)
    checkNamedGraph(uri2)
    println("In this case only the 2 triples for the timestamp")
  }

  /** check with SPARQL that endpoint is populated. */
  def checkNamedGraph(uri: String) = {
    val queryString = s"""
      # PREFIX foaf:
      CONSTRUCT {
        <$uri> ?P ?V .
        <$uri> <is:in> ?G .
      }
      WHERE {
        GRAPH ?G { <$uri> ?P ?V . }
      }"""
    //    println("queryString\n"+ queryString)
    rdfStore.r(dataset, {
      val result = for {
        query <- parseConstruct(queryString)
        es <- rdfStore.executeConstruct(dataset, query, Map())
      } yield es

      val r = result.get
      val size = r.size()
      println("checkNamedGraph size " + size)
      assert(size > 0)
    })
  }
}
