package deductions.sparql

import java.io.ByteArrayOutputStream
import java.net.URL

import scala.concurrent.duration.DurationInt
import scala.util.Try

import org.w3.banana.FOAFPrefix
import org.w3.banana.FutureW
import org.w3.banana.RDFModule
import org.w3.banana.RDFOpsModule
import org.w3.banana.SparqlHttpModule
import org.w3.banana.SparqlOpsModule
import org.w3.banana.TryW
import org.w3.banana.io.JsonLdCompacted
import org.w3.banana.io.RDFWriter
import org.w3.banana.jena.JenaModule

trait SPARQLHelperDependencies
  extends RDFModule
  with RDFOpsModule
  with SparqlOpsModule
  with SparqlHttpModule

trait SPARQLHelper extends SPARQLHelperDependencies {

  import ops._
  import sparqlOps._
  import sparqlHttp.sparqlEngineSyntax._

  implicit val jsonldCompactedWriter: RDFWriter[Rdf, Try, JsonLdCompacted]



  def runSparqlContructAsJSON(queryString: String, 
    endpoint: String = "http://dbpedia.org/sparql/"): String = {
    val answer = runSparqlContruct( queryString, endpoint)
        val os = new ByteArrayOutputStream
    val r = jsonldCompactedWriter.write(answer, os, endpoint)
    os.close()
    os.toString("utf-8")
  }

  def runSparqlContruct(queryString: String, 
    endpoint: String = "http://dbpedia.org/sparql/"): Rdf#Graph = {
    val endpointURL = new URL(endpoint)
    val ps = parseConstruct(queryString)
    val query0 = ps.asFuture
    val query: Rdf#ConstructQuery = parseConstruct(queryString).get
    val answer: Rdf#Graph = endpointURL.executeConstruct(query).getOrFail()
    answer
  }

  def runSparqlSelect(
    queryString: String, variables: Seq[String],
    endpoint: String = "http://dbpedia.org/sparql/"):
     List[Seq[Rdf#URI]] = {

    val endpointURL = new URL(endpoint)
    val query = parseSelect(queryString).get
    val answers: Rdf#Solutions = endpointURL.executeSelect(query).getOrFail(100 seconds)
    val results: Iterator[Seq[Rdf#URI]] = answers.iterator map {
      row =>
        for (variable <- variables) yield row(variable).get.as[Rdf#URI].get
    }
    results.to[List]
  }
}

object SPARQLAppWithJena
    extends JenaModule
    with App
    with SPARQLHelper {
  
  /** Here is an example doing some SPARQL. */
  val select = """
    PREFIX dbpedia: <http://dbpedia.org/resource/>
    select ?P
    WHERE {
      dbpedia:Reyrieux ?P ?O.
    } """
    val result = runSparqlSelect(select, Seq("P") )
    println(result.mkString("\n"))

    val foaf = FOAFPrefix[Rdf]
    val construct = s"""
      PREFIX foaf: <${foaf.prefixIri}>   
      CONSTRUCT {
        ?P <${foaf.familyName}> ?FN .
      }
      WHERE {
        ?P <${foaf.familyName}> ?FN .
      } LIMIT 10 """
     println( runSparqlContruct(construct) )
}

