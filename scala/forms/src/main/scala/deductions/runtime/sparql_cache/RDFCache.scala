package deductions.runtime.sparql_cache

import org.apache.jena.riot.RDFDataMgr
import org.w3.banana.CertPrefix
import org.w3.banana.DCPrefix
import org.w3.banana.DCTPrefix
import org.w3.banana.FOAFPrefix
import org.w3.banana.RDFModule
import org.w3.banana.RDFOpsModule
import org.w3.banana.RDFPrefix
import org.w3.banana.RDFSPrefix
import org.w3.banana.RDFXMLReaderModule
import org.w3.banana.TurtleReaderModule
import org.w3.banana.WebACLPrefix
import org.w3.banana.jena.JenaModule
import org.w3.banana.jena.JenaStore
import com.hp.hpl.jena.tdb.TDBFactory
import org.w3.banana.RDFStore
import org.w3.banana.GraphStore
import java.net.URL
import org.apache.log4j.Logger

/** */
trait RDFCache
  extends RDFModule
  with RDFOpsModule
  with TurtleReaderModule
  with RDFXMLReaderModule {
}
  
trait RDFCacheJena extends RDFCache with JenaModule {
//   self =>

  /** retrieve URI from a graph named by itself,
   * according to timestamp */
  def retrieveURI(uri: Rdf#URI, store: JenaStore) : Rdf#Graph = {
    null // TODO
  }
  /** store URI in a graph named by itself,
   *  and stores the timestamp TODO */
  def storeURI(uri: Rdf#URI, store: JenaStore) {
    store.writeTransaction {
      Logger.getRootLogger().info(s"storeURI uri $uri ")
      try{
      	val gForStore = store.getGraph(uri)
      	RDFDataMgr.read(gForStore, uri.toString()) // , N3)
      	println( "	stored")
      } catch {
      case t: Throwable => println( "ERROR: " + t )
      }
      //  val g = RDFXMLReader.read(
      //      v.toString(), v.toString() )
      //  store.execute {
      //    val ret = Command.append[Rdf]( v, g.toIterable ) // NOT COMPILING <<<<
      //    println(s"store executed: append on $v of {triples.size} triples")
      //    ret
      //  }
    }
    
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
} LIMIT 100
""")

    /* executes the query */
//    val answers: Rdf#Solutions = client.executeSelect(query).getOrFail()
//
//    /* iterate through the solutions */
//    val languages: Iterable[Rdf#URI] = answers.toIterable map { row =>
//      /* row is an Rdf#Solution, we can get an Rdf#Node from the variable name */
//      /* both the #Rdf#Node projection and the transformation to Rdf#URI can fail in the Try type, hense the flatMap */
//      row("language").flatMap(_.as[Rdf#URI]) getOrElse sys.error("die")
//    }
//
//    println(languages.toList)
  }
}

object RDFStore extends JenaModule {
  lazy val dataset = TDBFactory.createDataset("TDB")
  lazy val store = JenaStore(dataset, defensiveCopy = true)
}

}
