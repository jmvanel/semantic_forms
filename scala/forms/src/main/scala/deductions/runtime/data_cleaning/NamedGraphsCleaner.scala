package deductions.runtime.data_cleaning

import org.w3.banana.RDF
import deductions.runtime.services.SPARQLHelpers
import scala.util.Success
import scala.util.Failure

trait NamedGraphsCleaner[Rdf <: RDF, DATASET]
    extends SPARQLHelpers[Rdf, DATASET] {

  import ops._
  import sparqlOps._
  import rdfStore.sparqlEngineSyntax._
  import rdfStore.transactorSyntax._
  import rdfStore.sparqlUpdateSyntax._
  import rdfStore.transactorSyntax._
  import rdfStore.graphStoreSyntax._

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
            case Success(_) => val s = Some(s" : removed !") ; logger.debug(s) ; s
            case Failure(e) => val s = Some(s" : NOT removed ! $e") ; logger.debug(s) ; s
          }
        } else None
      },
      _ => None,
      _ => None)
  }
}