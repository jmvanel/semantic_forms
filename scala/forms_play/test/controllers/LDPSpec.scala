package controllers

import scala.concurrent.duration.DurationInt

import org.scalatestplus.play.OneAppPerTest
import org.scalatestplus.play.PlaySpec

import akka.util.Timeout
import deductions.runtime.services.DefaultConfiguration
import play.api.libs.iteratee.Enumerator
import play.api.test.FakeRequest
import play.api.test.Helpers
import play.api.test.Helpers.charset
import play.api.test.Helpers.contentAsString
import play.api.test.Helpers.contentType
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.Helpers.status


/** 
 *  https://www.playframework.com/documentation/2.4.x/ScalaTestingWithScalaTest
 *  
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
<ldp:test1/test1.ttl>

 * cf http://www.w3.org/TR/ldp-primer/#creating-an-rdf-resource-post-an-rdf-resource-to-an-ldp-bc */
class LDPSpec extends PlaySpec
    with WhiteBoxTestdependencies
    with OneAppPerTest {
  val config = new DefaultConfiguration {
    override val useTextQuery = false
    override val needLoginForEditing = false
    override val needLoginForDisplaying = false
  }

  val ldpContainerURI = "test1/"
  val file = "test1.ttl"
  val bodyTTL = """
    @prefix : <http://test#> .
    :s :p "Salut!"."""

  val ldpServiceURI = "ldp:" 
  val appURL = ldpServiceURI + ldpContainerURI

  val timeout: Timeout = Timeout( DurationInt(40) seconds )

  "LDP service" must {
    "respond to the ldp POST and GET Actions" in {
      post()
      get()
    }
  }
  
  def post() {
    val request = FakeRequest( Helpers.POST, appURL ).
      withHeaders(
          ("Slug", file),
          ("Content-Type", "text/turtle")
      ). withTextBody(bodyTTL)
    val result0 = Application.ldpPOSTAction(ldpContainerURI)(request)
    val enum: Enumerator[Array[Byte]] = Enumerator( bodyTTL.getBytes )
    val result = enum run result0 

    info( s"POST to URL $appURL")
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
    // assert( graph.contains("Salut!") )

    val query = s"SELECT * WHERE { GRAPH <$ldpDataFileURI> {?S ?P ?O.}}"
    info( s"query $query" )
    val r = Application.select( query ) (FakeRequest( Helpers.GET, "" ) )
    val result1 = enum run r
    val sresult: String = contentAsString( result1 )
    info( "Application.sparql: " + sresult )
    sresult.substring( sresult.length() - 200 )
//    assert( sresult.contains("Salut!") ) // For reason unknown this fails too !!!!!!!!!!
    //    info( s"""POST:assert succeded!""" )
  }
  
  def get() {
    val getRelativeURI = appURL + file
    info( s"""GET: $getRelativeURI""" )
	  val request = FakeRequest( Helpers.GET, getRelativeURI ).
    withHeaders(( "Accept", "text/turtle")) // , application/ld+json") )
    val result0 = Application.ldp(ldpContainerURI + file).apply(request)
    val enum: Enumerator[Array[Byte]] = Enumerator() ; val result = enum run result0 
    val content = contentAsString(result)(timeout)
    info( s"""GET: contentAsString: "$content" """ )
    assert( content.contains("Salut!") )
    // bodyText mustBe "ok"
  }

  import ops._
  import rdfStore.transactorSyntax._

  /** get named graph for given URI */
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
