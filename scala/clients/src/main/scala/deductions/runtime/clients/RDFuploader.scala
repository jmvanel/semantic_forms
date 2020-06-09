package deductions.runtime.clients

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
//import org.w3.banana.jena.JenaModule
import java.io.FileInputStream
import org.apache.jena.riot.system.StreamRDF
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.riot.system.StreamRDFBase
import org.apache.jena.graph.Triple
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.sparql.graph.GraphFactory
import org.apache.jena.riot.Lang
import java.io.StringWriter

/**
 * Load triples in given graph URI (SPARQL Load service);
 * splitting RDF file in chunks.
 * Arguments: RDF_FILE, LOAD_SERVICE, GRAPH, [MIME]
 * Content-Type [MIME] : application/ld+json application/rdf+xml text/turtle'
 * default value for arg. 4, Content-Type
 * 
 * IMPLEMENTATION NOTE: doing streaming and chunks would be nice,
 * but then this should occur both client and server side;
 * so this is for a future effort;
 * found no example on "akka http client streaming sending post"
 * see client side:
 * https://doc.akka.io/docs/akka-http/current/implications-of-streaming-http-entity.html
 * server:
 * https://richardimaoka.github.io/blog/akka-http-request-streaming/
 * Jena:
 * https://jena.apache.org/documentation/io/streaming-io.html
 * https://jena.apache.org/documentation/io/rdf-output.html
 */
object RDFuploader extends App
//with JenaModule
{
  val file = args(0)
  val uri = args(1)
  val graph = args(2)

  // split RDF file in chunks
  val triples = ArrayBuffer[Triple]()
  val destination: StreamRDF = new StreamRDFBase{
    var count = 0
    override def triple(triple: Triple ) {
      count += 1
      triples += triple
      if(count % 10000 == 0) {
        sendTriples(triples)
        triples.clear
        println(s"StreamRDF: sending triples, count=$count")
        Thread.sleep(10000)
      }
    }
  }
  RDFDataMgr.parse(destination, uri)
  // send remaining Triples
  sendTriples(triples)

  private def sendTriples(triples: ArrayBuffer[Triple]) = {
    import scala.concurrent.ExecutionContext.Implicits.global
//    Future {
      val graph = GraphFactory.createDefaultGraph()
      for (triple <- triples) graph.add(triple)
      val sw = new StringWriter()
      RDFDataMgr.write(sw, graph, Lang.NTRIPLES)
      val data: String = sw.toString()
      val uriAkka = Uri(uri+s"?graph=$graph")
      HttpRequest(method = HttpMethods.POST,
          uri = uriAkka,
        entity = HttpEntity(
          // MediaType.text("turtle", "turtle")
          MediaType.applicationWithFixedCharset("n-triples", HttpCharsets.`UTF-8`, "nt")
          // application/n-triples
          ,
          data))
      println(s"StreamRDF: sent triples, size ${triples.size}")
//    }
  }
}