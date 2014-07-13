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

trait JenaHelpers extends JenaModule {

    /** store URI in a graph named by itself,
   *  and stores the timestamp TODO */
  def storeURI(uri: Jena#URI, graphUri: Jena#URI, store: JenaStore) {
    store.writeTransaction {
      Logger.getRootLogger().info(s"storeURI uri $uri ")
      try{
      	val gForStore = store.getGraph(uri)
      	RDFDataMgr.read(gForStore, graphUri.toString())
      	println( "	stored")
      } catch {
      case t: Throwable => Logger.getRootLogger().error( "ERROR: " + t )
      }
      //  val g = RDFXMLReader.read(
      //      v.toString(), v.toString() )
      //  store.execute {
      //    val ret = Command.append[Rdf]( v, g.toIterable ) // NOT COMPILING <<<<
      //    println(s"store executed: append on $v of {triples.size} triples")
      //    ret
      //  }
    }
  }
  
  
  // TODO:

  import Ops._
  import SparqlOps._
  
  def storeURI2() {
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