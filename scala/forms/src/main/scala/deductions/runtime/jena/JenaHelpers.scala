package deductions.runtime.jena

import org.w3.banana.jena.JenaStore
import org.w3.banana.jena.Jena
import org.apache.log4j.Logger
import org.apache.jena.riot.RDFDataMgr
import org.w3.banana.RDFOpsModule
import org.w3.banana.SparqlOps
import org.w3.banana.SparqlHttp
import java.net.URL
import org.w3.banana.jena.JenaModule
import org.w3.banana.Command
import com.hp.hpl.jena.rdf.model.Model
import com.hp.hpl.jena.rdf.model.ModelFactory

/** Jena Helpers for JenaStore
 *  TODO pave the way for Banana 0.7 :
 *  - JenaStore is not existing anymore
 *  - generic API for transactions */
trait JenaHelpers extends JenaModule {

    /** store URI in a named graph, using Jena's RDFDataMgr
     * (use content-type or else file extension) 
     * with Jena Riot for smart reading of any format,
     * cf https://github.com/w3c/banana-rdf/issues/105
    */
    def storeURI(uri: Rdf#URI, graphUri: Rdf#URI, store: JenaStore) :
//    Model
    Rdf#Graph
    = {
    store.writeTransaction {
      Logger.getRootLogger().info(s"storeURI uri $uri graphUri $graphUri")
      try{
      	val gForStore = store.getGraph(graphUri)
      	val model = RDFDataMgr.loadModel(uri.toString())
      	
      	// not possible, as Banana also starts a transaction:
//      	store.appendToGraph( uri, model.getGraph() )
      	
      	store.dg.addGraph( uri, model.getGraph() )
      	
      	//	model.getNsPrefixMap // TODO use it to load referred vocab's
      	Logger.getRootLogger().info(s"storeURI uri $uri : stored")
      	model.getGraph
      } catch {
      case t: Throwable => Logger.getRootLogger().error( "ERROR: " + t )
      ModelFactory.createDefaultModel().getGraph
      }
    }
  }
      //  val g = RDFXMLReader.read( v.toString(), v.toString() )
      //  store.execute {
      //    val ret = Command.append[Rdf]( v, g.toIterable ) // NOT COMPILING <<<<
      //    println(s"store executed: append on $v of {triples.size} triples")
      //    ret
      //  }
    
  import Ops._
  import SparqlOps._
  
  // TODO:
  def SparqlQuery() {
    val client = SparqlHttp(new URL("http://dbpedia.org/sparql/"))
    /* creates a Sparql Select query */
    val query = SelectQuery("""
PREFIX ont: <http://dbpedia.org/ontology/>
SELECT DISTINCT ?language WHERE {
 ?language a ont:ProgrammingLanguage .
 ?language ont:influencedBy ?other .
 ?other ont:influencedBy ?language .
} LIMIT 100""")
    /* executes the query */
    //    val answers: Rdf#Solutions = client.executeSelect(query).getOrFail()
    //
    //    /* iterate through the solutions */
    //    val languages: Iterable[Rdf#URI] = answers.toIterable map { row =>
    //      /* row is an Rdf#Solution, we can get an Rdf#Node from the variable name */
    //      /* both the #Rdf#Node projection and the transformation to Rdf#URI can fail in the Try type, hense the flatMap */
    //      row("language").flatMap(_.as[Rdf#URI]) getOrElse sys.error("die")
    //    }
    //    println(languages.toList)
  }
}