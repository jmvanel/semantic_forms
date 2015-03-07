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
import org.w3.banana.SparqlGraphModule

trait SPARQLHelperDependencies
  extends RDFOpsModule
  with SparqlOpsModule
  with SparqlHttpModule
  with SparqlGraphModule

trait SPARQLHelper extends SPARQLHelperDependencies {

  import ops._
  import sparqlOps._
  import sparqlHttp.sparqlEngineSyntax._

  implicit val jsonldCompactedWriter: RDFWriter[Rdf, Try, JsonLdCompacted]

  def runSparqlContructAsJSON(queryString: String,
    endpoint: String = "http://dbpedia.org/sparql/"): String = {
    val answer = runSparqlContruct(queryString, endpoint)
    val os = new ByteArrayOutputStream
    val r = jsonldCompactedWriter.write(answer, os, endpoint)
    os.close()
    os.toString("utf-8")
  }

  def runSparqlContruct(queryString: String,
    endpoint: String = "http://dbpedia.org/sparql/"): Rdf#Graph = {
    val endpointURL = new URL(endpoint)
    val ps = parseConstruct(queryString)
    //    val query0 = ps.asFuture
    val query: Rdf#ConstructQuery = parseConstruct(queryString).get
    val answer: Rdf#Graph = endpointURL.executeConstruct(query).getOrFail()
    answer
  }

  /** HTTP Sparql Select */
  def runSparqlSelect(
    queryString: String, variables: Seq[String],
    endpoint: String = "http://dbpedia.org/sparql/"): List[Seq[Rdf#Node]] = {

    val endpointURL = new URL(endpoint)
    val query = parseSelect(queryString).get
    val answers: Rdf#Solutions = endpointURL.executeSelect(query).getOrFail(100 seconds)
    val results: Iterator[Seq[Rdf#Node]] = answers.iterator map {
      row =>
        for (variable <- variables) yield row(variable).get.as[Rdf#Node].get
    }
    results.to[List]
  }

  /** Sparql Select on a graph */
  def runSparqlSelect(
    queryString: String, variables: Seq[String],
    graph: Rdf#Graph): List[Seq[Rdf#Node]] = {
    import sparqlGraph.sparqlEngineSyntax._
    val query = parseSelect(queryString).get
    val answers: Rdf#Solutions = graph.executeSelect(query).get
    val results = answers.iterator map {
      row =>
        for (variable <- variables) yield row(variable).get.as[Rdf#Node].get
    }
    results.to[List]
  }
}
