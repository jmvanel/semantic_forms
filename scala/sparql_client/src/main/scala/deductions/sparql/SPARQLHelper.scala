package deductions.sparql

import org.w3.banana._
import org.w3.banana.diesel._
import java.net.URL
import org.w3.banana.jena.JenaModule
import java.io.OutputStream
import java.io.ByteArrayOutputStream
//import org.w3.banana.sesame.SesameModule
import scala.concurrent.duration._

/* declare your dependencies as a trait with all the modules you need */
trait SPARQLHelperDependencies
  extends RDFModule
  with RDFOpsModule
  with SparqlOpsModule
  with SparqlHttpModule
  with JsonLDWriterModule // TODO contribute this in Banana for Jena 

/** Here is an example doing some SPARQL. */
trait SPARQLHelper extends SPARQLHelperDependencies {

  import ops._
  import sparqlOps._
  import sparqlHttp.sparqlEngineSyntax._

//  runSparqlSelect("""
//    PREFIX ont: <http://dbpedia.org/ontology/>
//
//    SELECT DISTINCT ?language WHERE {
//    ?language a ont:ProgrammingLanguage .
//    ?language ont:influencedBy ?other .
//    ?other ont:influencedBy ?language .
//    } LIMIT 100
//    """,
//    Seq("language"))
  
  val foaf = FOAFPrefix[Rdf]
  runSparqlContruct( s"""
    PREFIX foaf: <${foaf.prefixIri}>   
    CONSTRUCT {
      ?P <${foaf.familyName}> ?FN .
    }
    WHERE {
      ?P <${foaf.familyName}> ?FN .
    } LIMIT 100
    """ )
  
  def runSparqlContruct( queryString:String,
        endpoint:String="http://dbpedia.org/sparql/" ) {
    
	  /* gets a SparqlEngine out of a Sparql endpoint */
    val endpointURL = new URL("http://dbpedia.org/sparql/")

    /* creates a Sparql Select query */
    println( s"sparqlOps $sparqlOps" )
    val ps = parseConstruct(queryString)
    val query0 = ps.asFuture
    val query : Rdf#ConstructQuery = parseConstruct(queryString).get
    val answer : Rdf#Graph = endpointURL.executeConstruct(query).getOrFail()
    println( answer )
    val os = new ByteArrayOutputStream
    val r = jsonldCompactedWriter.write(answer, os, endpoint )  
    os.close()
    println( os.toString("utf-8") )
  }
  
  def runSparqlSelect(
      queryString:String, vars:Seq[String],
      endpoint:String="http://dbpedia.org/sparql/"
      ) {
    
    /* gets a SparqlEngine out of a Sparql endpoint */
    val endpointURL = new URL("http://dbpedia.org/sparql/")

    /* creates a Sparql Select query */
    println( s"sparqlOps $sparqlOps" )
    val query = parseSelect(queryString).get

    /* executes the query */
    val answers: Rdf#Solutions = endpointURL.executeSelect(query).getOrFail(100 seconds)

    /* iterate through the solutions */
    for (varia <- vars) {
      val variables: Iterator[Rdf#URI] = answers.iterator map { row =>
        /* row is an Rdf#Solution, we can get an Rdf#Node from the variable name */
        /* both the #Rdf#Node projection and the transformation to Rdf#URI can fail in the Try type */
        row(varia).get.as[Rdf#URI].get
      }
      println(variables.to[List].mkString("\n"))
    }
  }
  
}

//object SPARQLAppWithSesame
//extends SesameModule 
//with App
//with SPARQLHelper

object SPARQLAppWithJena
extends JenaModule 
with App
with SPARQLHelper
 
