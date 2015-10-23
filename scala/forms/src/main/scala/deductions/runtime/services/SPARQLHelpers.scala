package deductions.runtime.services

import org.w3.banana.RDF
import org.w3.banana.TryW
import org.w3.banana.io.RDFWriter
import org.w3.banana.io.Turtle

import scala.util.Try
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

import deductions.runtime.dataset.RDFStoreLocalProvider

import java.io.ByteArrayOutputStream

import org.apache.log4j.Logger

/**
 * @author jmv
 */
trait SPARQLHelpers[Rdf <: RDF, DATASET] extends RDFStoreLocalProvider[Rdf, DATASET] {

  val turtleWriter: RDFWriter[Rdf, Try, Turtle]

  import ops._
  import sparqlOps._
  import rdfStore.transactorSyntax._
  import rdfStore.sparqlEngineSyntax._

  /**
   * NON transactional
   */
  def sparqlConstructQuery(queryString: String): Try[Rdf#Graph] = {
    val r = for {
      query <- {
    	  println( "sparqlConstructQuery: before parseConstruct" )
    	  parseConstruct(queryString)
      }
      es <- {
        println( "sparqlConstructQuery: before executeConstruct" )
        dataset.executeConstruct(query, Map())
      }
    } yield es
    r
  }

  /** transactional */
  def sparqlConstructQueryTR(queryString: String): String = {
    val transaction = dataset.r({
      graph2String( sparqlConstructQuery(queryString), "" )
    })
    transaction.get
  }
  
  /** transactional */
  def sparqlConstructQueryTR_old(queryString: String): String = {
    val transaction = dataset.r({
      val r = sparqlConstructQueryFuture(queryString)
      futureGraph2String(r, "")
    })
    transaction.get
  }

  /** transactional */
  def sparqlConstructQueryFuture(queryString: String): Future[Rdf#Graph] = {
    val r = sparqlConstructQuery( queryString )
    r.asFuture
  }

  /** transactional */
  def sparqlSelectQueryVariables(queryString: String, variables: Seq[String],
      ds: DATASET=dataset):
  List[Seq[Rdf#Node]] = {
    val transaction = ds.r({
      val solutionsTry = for {
        query <- parseSelect(queryString)
        es <- ds.executeSelect(query, Map())
      } yield es
      val answers: Rdf#Solutions = solutionsTry.get
      val results: Iterator[Seq[Rdf#Node]] = answers.iterator map {
        row =>
          for (variable <- variables) yield row(variable).get.as[Rdf#Node].get
      }
      results.to[List]
    })
    transaction .get
  }
    
  /** transactional */
  def sparqlSelectQuery(queryString: String,
            ds: DATASET=dataset): Try[List[Set[Rdf#Node]]] = {
    val transaction = ds.r({
      val solutionsTry = for {
        query <- parseSelect(queryString)
        es <- ds.executeSelect(query, Map())
      } yield es

      //    val answers: Rdf#Solutions = 
      val res = solutionsTry.map {
        solutions =>
          val results = solutions.iterator map {
            row =>
              val variables = row.varnames()
              //        println(variables.mkString(", "))
              //        println(row)
              for (variable <- variables) yield row(variable).get.as[Rdf#Node].get
          }
          results.to[List]
      }
      res
    })
    transaction.get
  }

  def futureGraph2String(triples: Future[Rdf#Graph], uri: String): String = {
    val graph = Await.result(triples, 5000 millis)
    Logger.getRootLogger().info(s"uri $uri ${graph}")
    val to = new ByteArrayOutputStream
    val ret = turtleWriter.write(graph, to, base = uri)
    to.toString
  }
  
  def graph2String(triples: Try[Rdf#Graph], baseURI: String): String = {
    Logger.getRootLogger().info(s"base URI $baseURI ${triples}")
    val ret = turtleWriter.asString(triples.get, base = baseURI)
    ret.get
}
}