package deductions.runtime.data_cleaning

import deductions.runtime.sparql_cache.SPARQLHelpers
import org.w3.banana.RDF

import scala.util.{Failure, Success}

trait NamedGraphsCleaner[Rdf <: RDF, DATASET]
    extends SPARQLHelpers[Rdf, DATASET] {

  import ops._

  val query = s"""
         |SELECT DISTINCT ?graph WHERE {
         |  graph ?graph {
         |    [] ?p ?O .
         |  }
         |}""".stripMargin
  val variables = Seq("graph")

  def cleanDBPediaGraphs() = {
    val namedGraphs = sparqlSelectQueryVariables(query, variables)
    println( s"namedGraphs $namedGraphs" )
    for (
      ngs <- namedGraphs;
      namedGraph <- ngs
    ) { removeGraphIfDBPedia(namedGraph) }
  }

  private def removeGraphIfDBPedia(namedGraph: Rdf#Node): Option[String] = {
    foldNode(namedGraph)(
      uri => {
        println(s"Considering Graph $uri");
        if (fromUri(uri).startsWith("http://dbpedia.org/resource/")) {
          val result = rdfStore.rw( dataset, {
            print(s"Remove Graph $uri")
          rdfStore.removeGraph(dataset, uri)
          })
          val res = result.flatten
          res  match {
            case Success(_) => val s = Some(s" : removed !") ; logger.debug(s.toString()) ; s
            case Failure(e) => val s = Some(s" : NOT removed ! $e") ; logger.debug(s.toString()) ; s
          }
        } else None
      },
      _ => None,
      _ => None)
  }
}