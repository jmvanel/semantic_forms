package deductions.runtime.sparql_cache

object RDFDashboard extends RDFCache with App {
  import ops._
  import sparqlOps._
  import rdfStore.sparqlEngineSyntax._

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