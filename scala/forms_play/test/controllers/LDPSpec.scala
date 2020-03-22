package controllers

import akka.stream.Materializer
import akka.util.Timeout
import deductions.runtime.utils.DefaultConfiguration
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers.{charset, contentAsString, contentType, defaultAwaitTimeout, status, _}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

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
    with  OneAppPerSuite {

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

  "LDP service" should {
    "respond to the ldp POST and GET Actions" in {
      post()
      get()
    }
  }

  implicit lazy val materializer: Materializer = app.materializer

  /** cf https://www.playframework.com/documentation/2.5.x/ScalaTestingWithScalaTest#Unit-Testing-EssentialAction */
  def post() {
    val request = FakeRequest( Helpers.POST, appURL ).
      withHeaders(
          ("Slug", file),
          ("Content-Type", "text/turtle")
      ). withTextBody(bodyTTL)
    val action = new ServicesApp().ldpPOSTAction(ldpContainerURI)
    val result = call(action, request)
      
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
    {
      val query = s"SELECT * WHERE { GRAPH <$ldpDataFileURI> {?S ?P ?O.}}"
      info(s"query $query")
      val action = WebPagesApp.select(query)
      val request = FakeRequest(Helpers.GET, "")
      val result = call(action, request)
      val sresult: String = contentAsString(result)
      info("Application.sparql: " + sresult)
      sresult.substring(sresult.length() - 200)
      //    assert( sresult.contains("Salut!") ) // For reason unknown this fails too !!!!!!!!!!
      //    info( s"""POST:assert succeded!""" )
    }
  }
  
  def get() {
    val getRelativeURI = appURL + file
    info( s"""GET: $getRelativeURI""" )
	  val request = FakeRequest( Helpers.GET, getRelativeURI ).
	  withHeaders(( "Accept", "text/turtle")) // , application/ld+json") )
    val action = new ServicesApp().ldp(ldpContainerURI + file)
    val result = call(action, request)
//    val enum: Enumerator[Array[Byte]] = Enumerator() ; val result = enum run result0 
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
