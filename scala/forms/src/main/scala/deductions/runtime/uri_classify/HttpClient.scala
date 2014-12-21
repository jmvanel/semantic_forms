package deductions.runtime.uri_classify

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.async.Async.{ async, await }
import akka.io.IO
import akka.util.Timeout
import akka.http.Http
import akka.actor.{ ActorSystem }
import akka.http.model.{ HttpMethods, HttpEntity, HttpRequest, HttpResponse, Uri }
import akka.stream.{ FlowMaterializer }
import akka.stream.scaladsl.Flow
import akka.pattern.ask
import HttpEntity._
import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Sink
import akka.http.model._
import akka.http.model.HttpMethods._
import java.net.URL
import akka.http.model.MediaRange.One

/* taken from 
 * akka-http-core/src/test/scala/akka/http/TestClient.scala
 * 
 * see http://doc.akka.io/docs/akka-stream-and-http-experimental/0.10/scala.html */
object HttpClient {
  implicit val askTimeout: Timeout = 10000.millis
  val testConf: Config = ConfigFactory.parseString("""
    akka.loglevel = INFO
    akka.log-dead-letters = off
    """)
  implicit val system = ActorSystem("deductions-HttpClient", testConf)
  import system.dispatcher
  implicit val materializer = FlowMaterializer()

  def makeRequest(url: String, method: HttpMethod) //  (implicit system: ActorSystem, materializer: FlowMaterializer)
  : Future[HttpResponse] = {
    val u = new URL(url)
    val host = u.getHost()
    val prot = u.getProtocol
    val urlPrefix = prot + "://" + host
    val uri = url.stripPrefix(urlPrefix)

    println(s"Fetching from HTTP server of host `$host` and URI $uri ...")
    def sendRequest(request: HttpRequest, connection: Http.OutgoingConnection): Future[HttpResponse] = {
      Source(List(HttpRequest() -> 'NoContext))
        .to(Sink(connection.requestSubscriber))
        .run()
      Source(connection.responsePublisher).map(_._1).runWith(Sink.head)
    }
    import scala.collection._
    val result = for {
      connection ← IO(Http).ask(Http.Connect(host)).mapTo[Http.OutgoingConnection]
      response ← sendRequest(HttpRequest(method, uri = uri,
        headers = immutable.Seq(
          headers.Accept(MediaRange.custom("text/turtle")),
          headers.Accept(MediaRange.custom("application/rdf+xml", qValue = 0.8f)),
          headers.Accept(MediaRange.custom("application/text", qValue = 0.5f))
        )), connection)
    } yield response // .header[headers.Server]

    result
  }
}