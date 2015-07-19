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
  wget --post-data=':s :p "Salut!".' \
    --header='Slug: test1.ttl' http://localhost:9000/ldp/test1/
    
    Or
    
  curl --request POST --data ':s :p "Salut!".' --header 'Slug: test1.ttl' \
    --header 'Content-type: text/turtle' http://localhost:9000/ldp/test1/  
 */
class LDPSpec extends FunSuite // Specification 
with JenaModule
    with RDFStoreLocalJena1Provider
    with RDFOpsModule
    with SparqlGraphModule
    with TurtleWriterModule 
    {

  val uri = "test1/"
  val file = "test1.ttl"
  val bodyTTL = """
    :s :p "Salut!".
    """
  val url = "ldp/" + uri
  
  test( "respond to the ldpPOST and ldp Actions" ) {
    val request = FakeRequest( Helpers.POST, url ).
      withHeaders(
          ("Slug", file),
    		  ("Content-type", "text/turtle")
      ).
      withBody(bodyTTL)
    val result0 = controllers.Application.ldpPOST(uri)(request)
    val result = result0.run

    val timeout: Timeout = Timeout( DurationInt(240) seconds )
    println( status(result)(timeout) ) // must equalTo(OK)
    println( contentType(result)(timeout) ) // must beSome("text/plain")
    println( charset(result)(timeout) ) // must beSome("utf-8")
    println( contentAsString(result)(timeout) ) // must contain(uri) 
    val g = getGraph(uri + file)
    println( s"""getGraph($uri + $file):
      $g""" )
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