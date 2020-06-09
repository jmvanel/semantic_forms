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
import scala.util.Success
import scala.util.Failure
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import scala.concurrent.Await
import scala.concurrent.duration.Duration

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
object RDFuploader extends App {
  val file = args(0)
  val loadServiceUri = args(1)
  val graphURI = args(2)
  val chunkSize = 10000
  val delayBetweenRequests = 10000 // milliseconds

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  // split RDF file in chunks
  val triples = ArrayBuffer[Triple]()
  val httpResponses = ArrayBuffer[Future[HttpResponse]]()
  val destination: StreamRDF = new StreamRDFBase{
    var count = 0
    override def triple(triple: Triple ) {
      count += 1
      triples += triple
      if(count % chunkSize == 0) {
        httpResponses += sendTriples(triples.toSeq)
        triples.clear
        logger.info(s"StreamRDF: sending triples, count=$count")
        Thread.sleep(delayBetweenRequests)
      }
    }
  }
  RDFDataMgr.parse(destination, file)
  // send remaining Triples
  httpResponses += sendTriples(triples toSeq)

  // terminate when all HTTP requests are done
  // cf https://stackoverflow.com/questions/29344430/scala-waiting-for-sequence-of-futures
  val httpResponsesTry = httpResponses.map(_.map { Success(_) }.recover { case t => Failure(t) })
  val httpResponsesFuture = Future.sequence(httpResponsesTry)
  logger.info(s"Awaiting until all HTTP ${httpResponsesTry.size} requests are done")
  Await.result(httpResponsesFuture, Duration("300 sec"))
  if(httpResponsesFuture isCompleted )
    system.terminate()
  else
    logger.warn(s"http Responses are NOT Completed")

  private def sendTriples(triplesChunk: Seq[Triple]): Future[HttpResponse] = {
    import scala.concurrent.ExecutionContext.Implicits.global
      val graph = GraphFactory.createDefaultGraph()
      for (triple <- triplesChunk) graph.add(triple)
      val sw = new StringWriter()
      RDFDataMgr.write(sw, graph, Lang.NTRIPLES)
      val data: String = sw.toString()
      val uriAkka = Uri(loadServiceUri+s"?graph=$graphURI")
    val httpRequest = HttpRequest(
      method = HttpMethods.POST,
      uri = uriAkka,
      entity = HttpEntity(
        MediaType.applicationWithFixedCharset("n-triples", HttpCharsets.`UTF-8`, "nt"),
        // MediaType.text("turtle", "turtle")
        data))
    val responseFuture: Future[HttpResponse] = Http().singleRequest(
          httpRequest)
    val triplesSize = triplesChunk.size
    responseFuture . onComplete {
        case Success(res) =>
          logger.info(s"HttpResponse: Success: size ${triplesSize}, ${res.toString()}")
        case Failure(f)   => sys.error(s"sendTriples: something wrong: $f")
      }
    logger.info(s"StreamRDF: future: send triples to $uriAkka, size ${triplesChunk.size}, responseFuture $responseFuture")
    responseFuture
  }
}