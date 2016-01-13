package deductions.runtime.services

import java.io.ByteArrayOutputStream

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import org.apache.log4j.Logger
import org.w3.banana.RDF
import org.w3.banana.TryW
import org.w3.banana.io.RDFWriter
import org.w3.banana.io.Turtle

import deductions.runtime.dataset.RDFStoreLocalProvider

/**
 * @author jmv
 */
trait SPARQLHelpers[Rdf <: RDF, DATASET]
extends RDFStoreLocalProvider[Rdf, DATASET] {

  val turtleWriter: RDFWriter[Rdf, Try, Turtle]

  import ops._
  import sparqlOps._
  import rdfStore.sparqlEngineSyntax._
  import rdfStore.transactorSyntax._
  import rdfStore.sparqlUpdateSyntax._
  import rdfStore.transactorSyntax._
  import rdfStore.graphStoreSyntax._

  /** sparql Construct Query;
   * NON transactional */
  def sparqlConstructQuery(queryString: String): Try[Rdf#Graph] = {
    val result = for {
      query <- {
    	  println( "sparqlConstructQuery: before parseConstruct" )
    	  parseConstruct(queryString)
      }
      es <- {
        println( "sparqlConstructQuery: before executeConstruct" )
        dataset.executeConstruct(query, Map())
      }
    } yield es
    result
  }

    /** sparql Update Query;
   * NON transactional */
  def sparqlUpdateQuery(queryString: String): Try[Unit] = {
    val result = for {
      query <- {
    	  println( "sparqlUpdateQuery: before parseUpdate" )
    	  parseUpdate( queryString )
      }
      es <- {
        println( "sparqlUpdateQuery: before executeUpdate" )
        dataset.executeUpdate( query, Map())
      }
    } yield es
    result
  }
  
  /** transactional */
  def sparqlConstructQueryTR(queryString: String): String = {
    val transaction = dataset.r({
      graph2String( sparqlConstructQuery(queryString), "" )
    })
    transaction.get
  }
  
//  /** transactional */
//  def sparqlConstructQueryTR_old(queryString: String): String = {
//    val transaction = dataset.r({
//      val r = sparqlConstructQueryFuture(queryString)
//      futureGraph2String(r, "")
//    })
//    transaction.get
//  }

  /** transactional */
  def sparqlConstructQueryFuture(queryString: String): Future[Rdf#Graph] = {
    val r = sparqlConstructQuery( queryString )
    r.asFuture
  }

  /**
   * replace all triples having same subject and property in Dataset;
   *  thus enforcing cardinality one
   *  No Transaction
   */
  def replaceRDFTriple(triple: Rdf#Triple, graphURI: Rdf#URI, dataset: DATASET) = {
    val uri = triple.subject
    val property = triple.predicate
    // TODO  WITH <g1> DELETE { a b c } INSERT { x y z } WHERE { ... }
    val queryString = s"""
         | DELETE {
         |   graph <$graphURI> {
         |     <$uri> <$property> ?ts .
         |   }
         | } WHERE {
         |   graph <$graphURI> {
         |     <$uri> <$property> ?ts .
         |   }
         | }""".stripMargin
    println(s"replaceRDFnode: sparqlUpdate Query: $queryString")
    val res = sparqlUpdateQuery(queryString)
    println(s"replaceRDFnode: sparqlUpdateQuery: $res")

    dataset.appendToGraph(graphURI, makeGraph(Seq(triple)))
  }

  //////////////// SELECT stuff //////////////////////////

  /** run SPARQL on given dataset, knowing result variables; transactional */
  def sparqlSelectQueryVariables(queryString: String, variables: Seq[String],
      ds: DATASET=dataset):
  List[Seq[Rdf#Node]] = {
    val transaction = ds.r({
      sparqlSelectQueryVariablesNT(queryString, variables, ds)
    })
    transaction .get
  }

  /** run SPARQL on given dataset, knowing result variables; NOT transactional */
  def sparqlSelectQueryVariablesNT(queryString: String, variables: Seq[String],
                                   ds: DATASET = dataset): List[Seq[Rdf#Node]] = {
    val solutionsTry = for {
      query <- parseSelect(queryString)
      es <- ds.executeSelect(query, Map())
    } yield es
//    println( "solutionsTry.isSuccess " + solutionsTry.isSuccess )
    val answers: Rdf#Solutions = solutionsTry.get
    val results = answers.iterator.toIterable map {
      row =>
//        println( row )
        for (variable <- variables) yield {
          val cell = row(variable)
          cell match {
            case Success(node) => row(variable).get.as[Rdf#Node].get
            case Failure(f)    => Literal(".")
          }
        }
    }
    results.to[List]
  }
  
  /** run SPARQL on given dataset; transactional
   * TODO the columns order may be wrong */
def sparqlSelectQuery(queryString: String,
            ds: DATASET=dataset): Try[List[List[Rdf#Node]]] = {
    val transaction = ds.r({
      val solutionsTry = for {
        query <- parseSelect(queryString)
        es <- ds.executeSelect(query, Map())
      } yield es
      val res = solutionsTry.map {
        solutions =>
          val results = solutions.iterator.toIterable map {
            row =>
              val variables = row.varnames().toList
//              println( row )
              for (variable <- variables) yield row(variable).get.as[Rdf#Node].get
          }
          println( "after results" )
          results.to[List]
      }
      println( "before res" )
      res
//      println( "after res" )
    })
    println( "before transaction.get" )
    transaction.get
  }
  
  /** run SPARQL on given graph, knowing result variables */
  def runSparqlSelect(
    queryString: String, variables: Seq[String],
    graph: Rdf#Graph): List[Seq[Rdf#URI]] = {

    val query = parseSelect(queryString).get
    val answers: Rdf#Solutions = sparqlGraph.executeSelect(graph, query,
        Map() ).get
    val results: Iterator[Seq[Rdf#URI]] = answers.toIterable map {
      row =>
        for (variable <- variables) yield row(variable).get.as[Rdf#URI].get
    }
    results.to[List]
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
  
  def dumpGraph( implicit graph: Rdf#Graph) = {
    val selectAll = """
              # CONSTRUCT { ?S ?P ?O . }
              SELECT ?S ?P ?O
              WHERE {
                GRAPH ?GR {
                ?S ?P ?O .
                } }
            """
    val res2 = runSparqlSelect(selectAll, Seq("S", "P", "O"), graph)
    info(s""" populateFromTDB selectAll size ${res2.size}
             ${res2.mkString("\n")}""")
  }
  
  def info(s: String) = Logger.getRootLogger().info(s)

}