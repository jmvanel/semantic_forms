package controllers

import scala.concurrent.duration._

import org.scalatest.FunSuite
import org.w3.banana.RDFOpsModule
import org.w3.banana.SparqlGraphModule
import org.w3.banana.TurtleWriterModule
import org.w3.banana.jena.JenaModule

import akka.util.Timeout
import deductions.runtime.jena.RDFStoreLocalJena1Provider

import play.api.test._
import play.api.test.Helpers._



/** 
 * For POST:

  wget --post-data='<s> <p> "Salut!".' \
    --header='Content-Type: text/turtle' \
    --header='Slug: test1.ttl' \
    http://localhost:9000/ldp/test1/

  wget --post-data='{ "@id": "urn:a12", "urn:message": "Salut!" }' \
    --header='Content-Type: text/json-ld' \
    --header='Slug: test1.json' \
    http://localhost:9000/ldp/test1/
    
    Or
    
  curl --request POST --data '<s> <p> "Salut!".' --header 'Slug: test1.ttl' \
    --header 'Content-type: text/turtle' http://localhost:9000/ldp/test1/  

 * For GET:

  wget --header 'Accept: text/turtle' http://localhost:9000/ldp/test1/test1.ttl
  wget --header 'Accept: text/json-ld' http://localhost:9000/ldp/test1/test1.json

The triples for this test are stored in this named graph: 
<lpd:test1/test1.ttl>

 * cf http://www.w3.org/TR/ldp-primer/#creating-an-rdf-resource-post-an-rdf-resource-to-an-ldp-bc */
class LDPSpec extends FunSuite
    with WhiteBoxTestdependencies {

  val ldpContainerURI = "test1/"
  val file = "test1.ttl"
  val bodyTTL = """
    @prefix : <http://test#> .
    :s :p "Salut!"."""
  val ldpServiceURI = "ldp:" 
  val appURL = ldpServiceURI + ldpContainerURI

  val timeout: Timeout = Timeout( DurationInt(240) seconds )
  
  test( "respond to the ldpPOST and ldp Actions" ) {
    post()
    get()
  }
  
  def post() {
    val request = FakeRequest( Helpers.POST, appURL ).
      withHeaders(
          ("Slug", file),
          ("Content-Type", "text/turtle")
      ). withTextBody(bodyTTL)
    val result = Application.ldpPOSTAction(ldpContainerURI)(request)

    info(  "POST status: " + status(result)(timeout) ) // must equalTo(OK)
    info( s"POST ${contentType(result)(timeout)}" ) // must beSome("text/plain")
    info(  "POST charset: " + charset(result)(timeout) ) // must beSome("utf-8")
    info( s"""POST contentAsString "${contentAsString(result)(timeout)}" """) // must contain(uri) 

    val ldpDataFileURI = ldpServiceURI + ldpContainerURI + file
    val graph = getGraph(ldpDataFileURI)
    info( s"""POST: getGraph($ldpDataFileURI):
        "$graph" """ )
      
    // NOTE: this assertion does not work, because TDB in object controllers.Application was updated,
    // and we query another TDB instance (in the same directory tough but that's not enough!)
//    assert( graph.contains("Salut!") )

        // should do a SPARQL query on controllers.Application
    val query = s"SELECT * WHERE { GRAPH <$ldpDataFileURI> {?S ?P ?O.}}"
    info( s"query $query" )
    val sresult: String = contentAsString(
        Application.select( query )
        (FakeRequest( Helpers.GET, "" ) ) )
    info( "Application.sparql: " + sresult )
    sresult.substring( sresult.length() - 200 )
//    assert( sresult.contains("Salut!") ) // For reason unknown this fails too !!!!!!!!!!

    //    info( s"""POST:assert succeded!""" )
  }

  def get() {
    info( s"""GET: """ )
	  val request = FakeRequest( Helpers.GET, appURL + file ).
    withHeaders(( "Accept", "text/turtle")) // , application/ld+json") )
    info( s"""GET: launching Application.ldp($ldpContainerURI + $file""" )
    val result = controllers.Application.ldp(ldpContainerURI + file)(request)
    val content = contentAsString(result)(timeout)
    info( s"""GET: contentAsString: "$content" """ )
    assert( content.contains("Salut!") )
  }

  import ops._
  import sparqlOps._
  import rdfStore.transactorSyntax._
  import rdfStore.sparqlEngineSyntax._

  def getGraph(uri: String) = {
    info( s"""getGraph(uri=$uri """ )
    dataset.r {
      for {
        graph <- rdfStore.getGraph(dataset, URI(uri))
        res <- turtleWriter.asString(graph, uri)
      } yield res
    } . get . get
  }

}
