package deductions.sparql

import org.scalatest.FunSuite
import org.w3.banana.FOAFPrefix
import org.w3.banana.jena.JenaModule

trait TestSPARQLHelper extends SPARQLHelper with TestFixtureRDF {
  self: FunSuite =>

  test("SPARQL HTTP select") {
    val select = """
    PREFIX dbpedia: <http://dbpedia.org/resource/>
    select ?P
    WHERE {
      dbpedia:Reyrieux ?P ?O.
    } """
    val result = runSparqlSelect(select, Seq("P"))
    println(result.take(10) mkString("\n"))
  }

  test("SPARQL graph select") {
    val select = """
    select ?S ?P ?O
    WHERE {?S ?P ?O .
    } """
      val result = runSparqlSelect(select, Seq("P"), graph)
      println("SPARQL graph select\n" + result.mkString("\n"))
  }

  test("SPARQL HTTP CONSTRUCT") {
    val foaf = FOAFPrefix[Rdf]
    val construct = s"""
      PREFIX foaf: <${foaf.prefixIri}>   
      CONSTRUCT {
        ?P <${foaf.familyName}> ?FN .
      }
      WHERE {
        ?P <${foaf.familyName}> ?FN .
      } LIMIT 10 """
    println(runSparqlContruct(construct))
  }
}
class TestSPARQLHelperWithJena
  extends FunSuite with JenaModule
  with TestSPARQLHelper
