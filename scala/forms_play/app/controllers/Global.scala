import java.net.URLDecoder
import java.net.URLEncoder
import scala.xml.Elem
import org.apache.log4j.Logger
import deductions.runtime.html.TableView
import deductions.runtime.jena.RDFStoreObject
import deductions.runtime.services.BrowsableGraph
import deductions.runtime.services.FormSaver
import deductions.runtime.services.StringSearchSPARQL
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.mvc.Controller
import play.api.mvc.Request
import deductions.runtime.html.CreationForm
import scala.concurrent.Future
import play.api.libs.iteratee.Enumerator
import org.w3.banana.io.RDFWriter
import org.w3.banana.jena.Jena
import org.w3.banana.io.Turtle
import deductions.runtime.uri_classify.SemanticURIGuesser
import deductions.runtime.sparql_cache.RDFCache
import org.w3.banana.RDFOpsModule
import scala.util.Try
import org.w3.banana.TurtleWriterModule

package global {

object Global extends Controller // play.api.GlobalSettings
  with RDFCache
   with RDFOpsModule
   with TurtleWriterModule
//   with SparqlGraphModule
//   with SparqlOpsModule
//   with RDFStoreLocalProvider
{
  var form : Elem = <p>initial value</p>
  lazy val tableView = new TableView {}
//  lazy val store =  RDFStoreObject.store
  lazy val search = new StringSearchSPARQL()
  lazy val dl = new BrowsableGraph()
  lazy val fs = new FormSaver()
  lazy val cf = new CreationForm { actionURI = "/save" }
  
  val hrefDisplayPrefix = "/display?displayuri="
  val hrefDownloadPrefix = "/download?url="

//  override def onStart(app: Application) {
//    val uri = "http://jmvanel.free.fr/jmv.rdf#me"
//    PopulateRDFCache.loadCommonVocabularies
//    form = htmlForm(uri)
//  }

  def htmlForm(uri0: String, blankNode:String="",
    editable:Boolean=false,
    lang:String="en" ) : Elem = {
      Logger.getRootLogger().info( s"""Global.htmlForm uri $uri0 blankNode "$blankNode" lang=$lang """ )
    val uri = uri0.trim()

    <div class="container">
			<div class="container">
				<div class="row">
					<h3>Properties for URI <b>{uri}</b></h3>
				</div>
				<div class="row">
      		<div class="col-md-6">
						<a href={uri} title="Download from original URI">Download from original URI</a>
					</div>
					<div class="col-md-6">
      			<a href={hrefDownloadPrefix + URLEncoder.encode(uri,"utf-8")}
       					 title="Download Turtle from database (augmented by users' edits)">Triples</a>
      		</div>
				</div>
			</div>
      {if (uri != null && uri != "")
        try {
          tableView.htmlForm(uri, hrefDisplayPrefix, blankNode, editable=editable,
              lang=lang ) . get
        } catch {
        case e:Exception => // e.g. org.apache.jena.riot.RiotException
          <p style="color:red">{
            e.getLocalizedMessage() + " " + printTrace(e)
            } <br/>
          Cause: { if( e.getCause() != null ) e.getCause().getLocalizedMessage()}
          </p>
        }
      else
        <div class="row">Enter an URI</div>
      }
      </div>
  }

  def displayURI2(uri:String) : Enumerator[scala.xml.Elem] = {
    val t2= SemanticURIGuesser.guessSemanticURIType("http://xmlns.com/foaf/0.1/")
    import scala.concurrent.ExecutionContext.Implicits.global
    val enum = Enumerator.enumerate( Seq(t2) )
//    Future.successful(t)
    // http://docs.scala-lang.org/overviews/core/futures.html
//    Enumerator.empty
    enum.map { x => <span>{ x.toString() }</span> }
    // TODO <<<<<<<<<<<<<<<<<<
  }
  
  def printTrace(e: Exception) : String = {
    var s = ""
    for ( i <- e.getStackTrace() ) { s = s + " " + i }
    s
  }
  
  def wordsearch(q:String="") : Elem = {
      <p>Searched for "{q}" :<br/>
    	  {search.search(q, hrefDisplayPrefix)}
      </p>
  }

  import scala.concurrent.ExecutionContext.Implicits.global
  def wordsearchFuture(q: String = ""): Future[Elem] = {
		  val f = search.search(q, hrefDisplayPrefix)
				  val xml = f . map { v =>
				  <p> Searched for "{ q }" :<br/>
				  {v} </p> }
		  xml
  }
    
  def downloadAsString( url:String ) : String = {
    println( "download url " + url )
    val res = dl.focusOnURI( url )  
    println( "download result " + res )
    res
  }

    def download(url: String) : Enumerator[Array[Byte]] = {
      // cf https://www.playframework.com/documentation/2.3.x/ScalaStream
      // and http://greweb.me/2012/11/play-framework-enumerator-outputstream/
      Enumerator.outputStream { os =>
        val graph = dl.search_only(url)
        graph.map { graph =>
          /* non blocking */
          val writer: RDFWriter[Jena, Try, Turtle] = turtleWriter
          val ret = writer.write(graph, os, base = url)
          os.close()
        }
      }
    }
  
  def edit( url:String ) : Elem = {
        htmlForm(url, editable=true)
  }

  def save(request: Request[_]): Elem = {
      val body = request.body
      body match {
        case form: AnyContentAsFormUrlEncoded =>
          val map = form.data
          println("Global.save: " + body.getClass + ", map " + map)
          try{
        	  fs.saveTriples(map)
          } catch {
          case t:Throwable => println( "Exception in saveTriples: " + t )
          // TODO show Exception to user
          }
          val uriOption = map.getOrElse("uri", Seq()).headOption
          println("Global.save: uriOption " + uriOption )
          uriOption match {
            case Some(url1) => htmlForm(
                URLDecoder.decode(url1, "utf-8"),
                editable = false )
            case _ => <p>Save: not normal: { uriOption }</p>
          }      
        case _ => <p>Save: not normal: { form.getClass() }</p>
      }
  }
  
  def create( uri0:String, lang:String="en"  ) : Elem = {
    Logger.getRootLogger().info("Global.htmlForm uri "+ uri0 )
    val uri = uri0.trim()
    
    <div class="container">
    	<h2>Creating an instance of Class <strong>{uri}</strong></h2>
      {cf.create(uri, lang).get}
		</div>
  }

  def sparql( query:String, lang:String="en"  ) : Elem = {
    Logger.getRootLogger().info("Global.sparql query  "+ query )
    <p>SPARQL query:<br/>{query}
    <br/>
    {dl.sparqlConstructQuery(query) /* TODO Future !!!!!!!!!!!!!!!!!!! */ }
    </p>
  }
 }
}