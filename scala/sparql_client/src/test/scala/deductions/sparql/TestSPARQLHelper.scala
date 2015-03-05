package deductions.sparql

import org.scalatest.FunSuite
import org.w3.banana.FOAFPrefix
import org.w3.banana.jena.JenaModule

trait TestSPARQLHelper extends SPARQLHelper {
  self: FunSuite =>

  test("SPARQL select") {
    val select = """
    PREFIX dbpedia: <http://dbpedia.org/resource/>
    select ?P
    WHERE {
      dbpedia:Reyrieux ?P ?O.
    } """
    val result = runSparqlSelect(select, Seq("P"))
    println(result.mkString("\n"))
  }

  test("SPARQL CONSTRUCT") {
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

object TestSPARQLHelperWithJena
    extends FunSuite with JenaModule
    with TestSPARQLHelper
