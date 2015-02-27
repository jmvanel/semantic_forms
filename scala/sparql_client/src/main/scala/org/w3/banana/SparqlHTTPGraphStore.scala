package org.w3.banana

//import java.net.URL

import org.w3.banana._
import scala.util.Try

trait SparqlQueryUpdateEngine[Rdf <: RDF, M[+_], A] extends SparqlEngine[Rdf, M, A] with SparqlUpdate[Rdf, M, A]

/**
 * Graph Store backed by a concrete SPARQL engine `SE`, which can be:
 *  - an HTTP endpoint so `SE` is then java.net.URL,
 *  - or an embedded SPARQL engine (e.g. a Jena TDB Dataset);
 *
 * That is, we simulate a Banana-RDF GraphStore through a Banana-RDF SparqlEngine `SE`,
 *  for the added flexibility for the use cases:
 *  - UI for a SPARQL database administrator
 *  - data server backed either by embedded SPARQL engine or HTTP SPARQL engine
 *
 * We use the same type `Rdf` for 2 purposes:
 *  1) for constructing local Rdf fragments
 *  2) for sending HTTP SPARQL requests;
 */
class SparqlGraphStore[Rdf <: RDF, M[+_], SE](sparqlEngine: SparqlQueryUpdateEngine[Rdf, M, SE])(implicit ops: RDFOps[Rdf],
  sparqlOps: SparqlOps[Rdf])
    extends GraphStore[Rdf, M, SE] {

  // Members declared in org.w3.banana.GraphStore

  def appendToGraph(a: SE, uri: Rdf#URI, graph: Rdf#Graph): M[Unit] = {
    val triples = ops.getTriples(graph)
    val tripless = triples.mkString("\n")
    runUpdate(s"""INSERT DATA
    		{ GRAPH <$uri> {
          $tripless
          }""", a)
  }

  def getGraph(a: SE, uri: Rdf#URI): M[Rdf#Graph] = {
    runConstruct(s"CONSTRUCT { ?s ?p ?o } WHERE { GRAPH <$uri> { ?s ?p ?o } }", a)
  }

  def removeGraph(a: SE, uri: Rdf#URI): M[Unit] = {
    runUpdate(s"DROP GRAPH <$uri>", a)
  }

  def removeTriples(a: SE, uri: Rdf#URI, triples: Iterable[org.w3.banana.TripleMatch[Rdf]]): M[Unit] = {
    val tripless = triples.mkString("\n")
    runUpdate(s"""DELETE DATA
    		{ GRAPH  <$uri>  {
          $tripless
          }""", a)
  }

  private def runUpdate(query: String, a: SE) = {
    val updateQuery: Rdf#UpdateQuery = sparqlOps.parseUpdate(query, prefixes = Seq()).get
    val bindings = Map[String, Rdf#Node]()
    sparqlEngine.executeUpdate(a, updateQuery, bindings)
  }
  private def runConstruct(query: String, a: SE) = {
    val cQuery = sparqlOps.parseConstruct(query, prefixes = Seq()).get
    val bindings = Map[String, Rdf#Node]()
    sparqlEngine.executeConstruct(a, cQuery, bindings)
  }
}
