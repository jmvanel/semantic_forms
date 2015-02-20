package deductions.runtime.services

import org.w3.banana.SparqlEngine
import org.w3.banana.RDF
import org.w3.banana.RDFOps
import org.w3.banana.SparqlOps
import org.w3.banana.RDFStore
import scala.xml.Elem
import scala.concurrent._
import scala.concurrent.util._
import scala.concurrent.ExecutionContext.Implicits.global
import deductions.runtime.html.Form2HTML
import scala.concurrent.duration._
import org.apache.log4j.Logger
import deductions.runtime.jena.RDFStoreObject
import scala.util.Try
import org.w3.banana._
import org.w3.banana.syntax._
import deductions.runtime.abstract_syntax.InstanceLabelsInference2
import deductions.runtime.dataset.RDFStoreLocalProvider

/** String Search with simple SPARQL */
trait StringSearchSPARQL[Rdf <: RDF, DATASET]
    extends InstanceLabelsInference2[Rdf] with SparqlOpsModule
    with RDFStoreLocalProvider[Rdf, DATASET] {

  import ops._
  import sparqlOps._

  def search(search: String, hrefPrefix: String = ""): Future[Elem] = {
    val uris = search_only(search)
    val elem = uris.map(
      u => displayResults(u.toIterable, hrefPrefix))
    elem
  }

  /** CAUTION: It is of particular importance to note that, unless stated otherwise, one should never use an iterator after calling a method on it. */
  private def displayResults(res0: Iterable[Rdf#Node], hrefPrefix: String) = {
    <p>{
      val res = res0.toSeq
      println(s"displayResults : ${res.mkString("\n")}")

      implicit val graph: Rdf#Graph = allNamedGraph
      //        val all = ops.find( allNamedGraph, ANY, ANY, ANY)
      //        println( all.mkString("\n") )

      res.map(uri => {
        val uriString = uri.toString
        val blanknode = !isURI(uri)
        <div title=""><a href={ Form2HTML.createHyperlinkString(hrefPrefix, uriString, blanknode) }>
                        {
                          //                        	  uriString
                          instanceLabel(uri)
                        }
                      </a><br/></div>
      })
    }</p>
  }

  /**
   * NOTE: this stuff is pretty generic;
   *  just add these arguments :
   *  queryString:String, vars:Seq[String]
   */
  private def search_only(search: String): Future[Iterator[Rdf#Node]] = {
    val queryString =
      s"""
         |SELECT DISTINCT ?thing WHERE {
         |  graph ?g {
         |    ?thing ?p ?string .
         |    FILTER regex( ?string, "$search", 'i')
         |  }
         |}""".stripMargin

    val transaction =
      rdfStore.rw(dataset, {
        var i = 0
        val result = for {
          query <- parseSelect(queryString)
          solutions <- rdfStore.executeSelect(dataset, query, Map())
        } yield {
          solutions.toIterable.map {
            row =>
              i = i + 1
              println(s"search_only : $i $row")
              row("thing") getOrElse sys.error("")
          }
        }
        //        println( """result.get.mkString("")""" ) 
        //        println( result.get.mkString("") ) 
        result
      })
    val tryIteratorRdfNode = transaction.flatMap { identity }
    tryIteratorRdfNode.asFuture
  }

  def isURI(node: Rdf#Node) = ops.foldNode(node)(identity, x => None, x => None) != None

}