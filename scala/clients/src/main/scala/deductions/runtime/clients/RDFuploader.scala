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
import akka.http.scaladsl.unmarshalling.Unmarshal
import scala.util.Try

/**
 * Load triples in given graph URI (SPARQL Load service);
 * splitting RDF file in chunks.
 * Arguments:
 * RDF_FILE, LOAD_SERVICE, GRAPH,
 * starting triple: allows to start sending after given triple number
 *
 * (not implemented) [MIME]
 * Content-Type [MIME] : application/ld+json application/rdf+xml text/turtle'
 * default value for arg. 4, Content-Type
 * 
 * IMPLEMENTATION NOTES:
 * - no dependency to Banana, Jena's StreamRDF is needed
 * - doing streaming and chunks would be nice,
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
  val startingTriple = args.lift(3).getOrElse("0").toInt
  val mimeInput = args.lift(4).getOrElse("turtle") // TODO
  logger.info( s"startingTriple=$startingTriple")

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
      if(count >= startingTriple)
        triples += triple
//      println(s"count $count, size=${triples.size}")
      if(count % chunkSize == 0 ) {
        logger.info(s"StreamRDF: position=$count , size=${triples.size}")
        if (count >= startingTriple && triples.size > 0) {
          var reTry = true
          var reTryCount = 1
          do {
            val (httpResponse, optionThrowable) = sendTriples(triples.toSeq, count)
            logger.info(s"StreamRDF: send triples, position=$count , size=${triples.size}")
            optionThrowable match {
              case Failure(error) =>
                logger.info(s"StreamRDF: RETRY, position=$count , size=${triples.size}")
                Thread.sleep(delayBetweenRequests)
                reTry = true
                reTryCount = reTryCount+1
                println(s"reTryCount $reTryCount")
                if( reTryCount >= 5 ) {
                  println(s"Too mny retries with server <$loadServiceUri> , quitting!")
                  System.exit(1)
                }
              case Success(s) =>
                httpResponses += httpResponse
                reTry = false
            }
          } while (reTry)
        }
        triples.clear
        Thread.sleep(delayBetweenRequests)
      }
    }
  }
  RDFDataMgr.parse(destination, file)
  // send remaining Triples
  val (httpResponse, optionThrowable) = sendTriples(triples toSeq, Int.MaxValue)
  httpResponses += httpResponse


  // terminate when all HTTP requests are done
  // cf https://stackoverflow.com/questions/29344430/scala-waiting-for-sequence-of-futures
  val httpResponsesTry = httpResponses.map(_.map { Success(_) } ) // .recover { case t => Failure(t) })
  val httpResponsesFuture = Future.sequence(httpResponsesTry)
  logger.info(s"Awaiting until all HTTP ${httpResponsesTry.size} requests are done")
  Await.result(httpResponsesFuture, Duration("300 sec"))
  if(httpResponsesFuture isCompleted )
    system.terminate()
  else
    logger.warn(s"http Responses are NOT Completed")

  /** send Triples in a Future */
  private def sendTriples(triplesChunk: Seq[Triple], count:Int): ( Future[HttpResponse],
      Try[String]
  ) = {
    import scala.concurrent.ExecutionContext.Implicits.global
    import scala.concurrent.duration.DurationInt
      val graph = GraphFactory.createDefaultGraph()
      for (triple <- triplesChunk) graph.add(triple)
      val sw = new StringWriter()
      RDFDataMgr.write(sw, graph,
          // TODO use mimeInput
          Lang.NTRIPLES)
      val data: String = sw.toString()
      val uriAkka = Uri(loadServiceUri+s"?graph=$graphURI&message=count-$count")
    val httpRequest = HttpRequest(
      method = HttpMethods.POST,
      uri = uriAkka,
      entity = HttpEntity(
        MediaType.applicationWithFixedCharset("n-triples", HttpCharsets.`UTF-8`, "nt"),
        // MediaType.text("turtle", "turtle")
        data))
    val responseFuture: Future[HttpResponse] = Http().singleRequest(httpRequest)
    val triplesSize = triplesChunk.size
    var optionThrowable : Try[String] = Success("Initial value")
    val waited = Try { Await.result(responseFuture, 20000 millis) }
    println( s"waited $waited")
    responseFuture.onComplete {
      case Success(res) =>
        val mess = s"HttpResponse: Success: count $count, size ${triplesSize}, ${res.toString()}, entity ${Unmarshal(res.entity).to[String]}"
        logger.info(mess)
        optionThrowable = Success(mess)
      case Failure(f) =>
        sys.error(s"sendTriples: something wrong: count $count, $f")
        optionThrowable = Failure(f)
    }
    // logger.info(s"StreamRDF: future: send triples to $uriAkka, count $count, size ${triplesSize}, responseFuture $responseFuture")
    println( s"optionThrowable $optionThrowable")
    val optionThrowableCombined : Try[String] = optionThrowable . map { s => s + " - " + waited.toString }
    println( s"optionThrowableCombined $optionThrowableCombined")
    // NOTE: don't like this but somehow in case of ConnectException in Await, optionThrowable is not updated ...
    val returnedTry = if (optionThrowableCombined . isSuccess && waited . isFailure )
      waited . map { resp => resp.toString() }
    else optionThrowableCombined
    ( responseFuture, returnedTry)
  }
}
