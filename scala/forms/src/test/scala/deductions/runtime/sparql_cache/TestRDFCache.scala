package deductions.runtime.sparql_cache

import deductions.runtime.jena.{RDFCache, RDFStoreLocalJenaProvider}
import deductions.runtime.utils.{DefaultConfiguration, FileUtils}
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import org.w3.banana.SparqlOpsModule
import org.w3.banana.jena.JenaModule

//@Ignore
class TestRDFCache extends FunSuite with RDFCache
    with SparqlOpsModule
    with BeforeAndAfterAll
    with JenaModule
    with RDFStoreLocalJenaProvider
    with SitesURLForDownload {

  val config = new DefaultConfiguration {
    override val useTextQuery = false
  }
  val uri = "http://jmvanel.free.fr/jmv.rdf#me"
  //  val uri2 = "http://live.dbpedia.org/page/Taraxacum_japonicum"
  //  val uri2 = uri 
  val uri2 = githubcontent + "/jmvanel/rdf-i18n/master/foaf/foaf.fr.ttl"

  import ops._
  import sparqlOps._

  override def afterAll {
    // FileUtils.deleteLocalSPARQL()
    close()
    close(dataset2)
    println("afterAll: close TDB & TDB2")
  }

  test("save to enpoint cache and check with SPARQL that endpoint is populated.") {
    val uriNode = makeUri(uri)
    println("test 1")
    val gr = retrieveURI(uriNode, dataset)
    println("test 2")
    println(s"rdfStore: $rdfStore")
    val r = rdfStore.r(dataset, {
      println(s"gr: $gr")
      println("graph from " + uri + " size " + gr.getOrElse(emptyGraph).size)
      val g = rdfStore.getGraph(dataset, uriNode)
      g
    })
    //    println("rdfStore.getGraph( dataset, uriNode).get " + g.get)
    checkNamedGraph(uri)
  }

  test("save to enpoint cache with storeURI() and check with SPARQL.") {
    val uriNode = makeUri(uri2)
    println(s"test 3 $uriNode")

    readStoreUriInNamedGraph(uriNode)
    println("test 4")
    checkNamedGraph(uri2)
    println("In this case only the 2 triples for the timestamp")
  }

  /** check with SPARQL that endpoint is populated. */
  def checkNamedGraph(uri: String) = {
    val queryString = s"""
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
