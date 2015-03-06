package deductions.runtime.jena

import org.w3.banana.RDFOpsModule
import org.w3.banana.SparqlGraphModule
import org.scalatest.FunSuite
import org.w3.banana.jena.JenaModule
import org.w3.banana.RDF
import org.w3.banana.jena.Jena
import com.hp.hpl.jena.query.Dataset
import deductions.runtime.dataset.RDFStoreLocalProvider
import org.w3.banana.SparqlEngine
import org.w3.banana.SparqlOps
import scala.util.Try

trait TestRDFStoreObject[Rdf <: RDF, DATASET]
extends FunSuite with RDFOpsModule
with SparqlGraphModule
{
  import ops._
  implicit val sparqlGraph: SparqlEngine[Rdf, Try, Rdf#Graph]
  implicit val sparqlOps: SparqlOps[Rdf]

  import ops._
  import sparqlOps. _
  import sparqlGraph._
  import sparqlGraph.sparqlEngineSyntax._
  
//  def makeRDFStore(): RDFStoreLocalProvider[Rdf, DATASET]
  def makeGraph(): Rdf#Graph

  test("SPARQL queries on RDFStoreObject.allNamedGraph") {
    println("Entering SPARQL queries on RDFStoreObject.allNamedGraph")
//    val store: RDFStoreLocalProvider[Rdf, DATASET] = makeRDFStore()
//    val graph = store.allNamedGraph
    val graph = makeGraph()
    val queryWithoutGRAPH = s"""
                prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                SELECT *
                WHERE {
                  ?S ?P ?O . 
                }
                """
    val queryWithGRAPH = s"""
                prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                SELECT *
                WHERE {
                  GRAPH ?GR {
                    ?S ?P ?O . 
                  }
                }
                """
    println("\nresult Without GRAPH {}")
    val query = parseSelect(queryWithoutGRAPH, Seq()).get
    val solutions: Rdf#Solutions = graph.executeSelect(query).get
//    val solutions: Rdf#Solutions = sparqlGraph.executeSelect(graph, query, Map()).get
    printSolutions(solutions)
    
    println("\nresult With GRAPH {} : EMPTY !!!")
    val query2 = parseSelect(queryWithGRAPH, Seq()).get
    printSolutions( graph.executeSelect(query2).get )
    // EMPTY !!!
    
    val resFind = find(graph, ANY, ANY, ANY)
    println(s"""result of Find
      ${resFind.take(10).mkString("\n")} """)
  }
  
  def printSolutions( solutions: Rdf#Solutions ) = {      
    val res = solutions.iterator() map {
      row =>
//        info(s""" populateFromTDB iter ${row}""")
        ( row("S").get.as[Rdf#Node].get,
          row("P").get.as[Rdf#Node].get,
          row("O").get.as[Rdf#Node].get )
    }
    val values = res.to[List]
    println(s"""values
      ${values.take(10).mkString("\n")} """)
  }
}

class TestRDFStoreObjectJena extends TestRDFStoreObject[Jena, Dataset] with RDFStoreLocalJena1Provider with JenaModule {
  def makeGraph(): Rdf#Graph = {
		println(s"""dataset $dataset """)
    allNamedGraph
//    dataset.getNamedModel( "Person" ).getGraph
  }
//  def makeRDFStore(): RDFStoreLocalProvider[Rdf, DATASET] = RDFStoreObject
}
