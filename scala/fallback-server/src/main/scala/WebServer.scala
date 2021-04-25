import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import scala.io.StdIn

/** Web Server for maintenance times.
 *  see https://doc.akka.io/docs/akka-http/current/introduction.html */
object WebServer {
  def main(args: Array[String]) {

    val port = args . lift(0).getOrElse("8080").toInt
    val mess = args . lift(1).getOrElse("")

    val startTime = new java.util.Date
    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val route =
      path("") {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`,
            s"""
            <h1>Ce site est en maintenance</h1>
            Soyez patients...

           <h1>This site is in maintenance</h1>
            Be patient...
            <hr/>
            $startTime
            <p>
            $mess
            </p>
            """))
        }
      }

    val bindingFuture = Http().bindAndHandle(route, "localhost", port)

    println(s"Server online at http://localhost:$port/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
