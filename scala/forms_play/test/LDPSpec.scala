package deductions.test

/**
 * 
 * @author jmv
 */

import play.api.test._
import play.api.test.Helpers._
import deductions.runtime.jena.RDFCache
import org.w3.banana.RDFOpsModule
import org.w3.banana.SparqlGraphModule
import org.w3.banana.jena.JenaModule
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import org.w3.banana.TurtleWriterModule
import org.scalatest.FunSuite
import akka.util.Timeout
import scala.concurrent.duration._

/** 
 * For POST:

  wget --post-data='<s> <p> "Salut!".' \
    --header='Content-Type: text/turtle' \
    --header='Slug: test1.ttl' \
    http://localhost:9000/ldp/test1/

  wget --post-data='{ "@id": "urn:a12", "message": "Salut!" }' \
    --header='Content-Type: text/json-ld' \
    --header='Slug: test1.json' \
    http://localhost:9000/ldp/test1/
    
    Or
    
  curl --request POST --data '<s> <p> "Salut!".' --header 'Slug: test1.ttl' \
    --header 'Content-type: text/turtle' http://localhost:9000/ldp/test1/  

 * For GET:

  wget --header 'Accept: text/turtle' http://localhost:9000/ldp/test1/test1.ttl
  wget --header 'Accept: text/json-ld' http://localhost:9000/ldp/test1/test1.json

 * */
class LDPSpec extends FunSuite // Specification 
with JenaModule
    with RDFStoreLocalJena1Provider
    with RDFOpsModule
    with SparqlGraphModule
    with TurtleWriterModule
    {

  val ldpContainerURI = "test1/"
  val file = "test1.ttl"
  val bodyTTL = """
    @prefix : <http://test#> .
    :s :p "Salut!".
    """
  val appURL = "ldp/" + ldpContainerURI
  val timeout: Timeout = Timeout( DurationInt(240) seconds )
  
  test( "respond to the ldpPOST and ldp Actions" ) {
    post()
    get()
  }
  
  /** cf http://www.w3.org/TR/ldp-primer/#creating-an-rdf-resource-post-an-rdf-resource-to-an-ldp-bc
   *  
Link: <http://www.w3.org/ns/ldp#Resource>; rel="type"
Slug: foaf
Content-Type: text/turtle

* */
  def post() {
    val request = FakeRequest( Helpers.POST, appURL ).
      withHeaders(
          ("Slug", file),
          ("Content-Type", "text/turtle")
      ). withTextBody(bodyTTL)
    val result = controllers.Application.ldpPOST(ldpContainerURI)(request)

    info( "status: " + status(result)(timeout) ) // must equalTo(OK)
    println( contentType(result)(timeout) ) // must beSome("text/plain")
    info( "charset: " + charset(result)(timeout) ) // must beSome("utf-8")
    println( contentAsString(result)(timeout) ) // must contain(uri) 
    val graph = getGraph(ldpContainerURI + file)
    info( s"""POST: getGraph($ldpContainerURI + $file):
      $graph""" )
    assert( graph.contains("Salut!") )
  }

  def get() {
	  val request = FakeRequest( Helpers.GET, appURL + file ).
    withHeaders(( "Accept", "text/turtle")) // , application/ld+json") )
    val result = controllers.Application.ldp(ldpContainerURI + file)(request)
    val content = contentAsString(result)(timeout)
    info( "GET: contentAsString: " + content )
    assert( content.contains("Salut!") )
  }

  import ops._
  import sparqlOps._
  import rdfStore.transactorSyntax._
  import rdfStore.sparqlEngineSyntax._

  def getGraph(uri: String) = {
    dataset.r {
      for {
        graph <- rdfStore.getGraph(dataset, URI(uri))
        res <- turtleWriter.asString(graph, uri)
      } yield res
    } . get . get
  }

}
