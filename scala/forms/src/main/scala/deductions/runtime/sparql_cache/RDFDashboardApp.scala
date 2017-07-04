package deductions.runtime.sparql_cache
//import deductions.runtime.jena.JenaHelpers
import deductions.runtime.DependenciesForApps
import deductions.runtime.services.DefaultConfiguration

object RDFDashboardApp
    extends {
      override val config = new DefaultConfiguration {
        override val useTextQuery = false
      }
    } with DependenciesForApps {

  import ops._
  import rdfStore.sparqlEngineSyntax._
  import sparqlOps._

  // TODO show # of triples
  val queryString =
    s"""
         |SELECT DISTINCT ?g WHERE {
         |  graph ?g {
         |    [] ?p ?O .
         |  }
         |}""".stripMargin
  val result = for {
    query <- parseSelect(queryString)
    solutions <- dataset.executeSelect(query, Map())
  } yield {
    solutions.toIterable.map {
      row => row("g") getOrElse sys.error("RDFDashboard: " + row)
    }
  }
  println(s"${result.get.mkString("\n")}")

}