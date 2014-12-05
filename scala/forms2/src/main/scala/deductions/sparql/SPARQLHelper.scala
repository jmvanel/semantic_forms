package deductions.sparql

import org.w3.banana._
import org.w3.banana.diesel._
import java.net.URL

/* declare your dependencies as a trait with all the modules you need
 */
trait SPARQLHelperDependencies
  extends RDFModule
  with RDFOpsModule
  with SparqlOpsModule
  with SparqlHttpModule

/* Here is an example doing some IO. Read below to see what's
 * happening.
 * 
 * As you can see, we never use Jena nor Sesame directly. The binding
 * is done later by providing the module implementation you
 * want. Hopefully, you'll have the same results :-)
 * 
 * To run this example from sbt:
 *   project examples
 *   run-main org.w3.banana.examples.SPARQLExampleWithJena
 */
trait SPARQLHelper extends SPARQLHelperDependencies 
// with App
{
//  self =>

  import ops._
  import sparqlOps._
  import sparqlHttp.sparqlEngineSyntax._

  runSparql("""
    PREFIX ont: <http://dbpedia.org/ontology/>

    SELECT DISTINCT ?language WHERE {
    ?language a ont:ProgrammingLanguage .
    ?language ont:influencedBy ?other .
    ?other ont:influencedBy ?language .
    } LIMIT 100
    """,
    Seq("language")
    )
  
  def runSparql( queryString:String, vars:Seq[String] ) {
    
    /* gets a SparqlEngine out of a Sparql endpoint */
    val endpoint = new URL("http://dbpedia.org/sparql/")

    /* creates a Sparql Select query */
    println( s"sparqlOps $sparqlOps" )
    val query0 = parseSelect(queryString)
    val query = query0.get

    /* executes the query */
    val answers: Rdf#Solutions = endpoint.executeSelect(query).getOrFail()

    /* iterate through the solutions */
    for (varia <- vars) {
      val variables: Iterator[Rdf#URI] = answers.iterator map { row =>
        /* row is an Rdf#Solution, we can get an Rdf#Node from the variable name */
        /* both the #Rdf#Node projection and the transformation to Rdf#URI can fail in the Try type,
    	 * hence the flatMap */
        row(varia).get.as[Rdf#URI].get
      }
      println(variables.to[List].mkString("\n"))
    }
  }
  
}

/* Here is how you instantiate your modules. Note that this is using
 * the default module implementations in banana-rdf but nothing is
 * preventing you from implementing your own or re-using part of
 * them. */

import org.w3.banana.jena.JenaModule

object SPARQLAppWithJena 
extends JenaModule 
with App
with SPARQLHelper
 
