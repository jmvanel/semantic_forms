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
import deductions.runtime.utils.RDFHelpers0
import org.w3.banana.io.JsonLdFlattened
import org.w3.banana.io.JsonLdExpanded
import org.w3.banana.io.JsonLdCompacted

/**
 * @author jmv
 */
trait SPARQLHelpers[Rdf <: RDF, DATASET]
    extends RDFStoreLocalProvider[Rdf, DATASET]
    		with RDFHelpers0[Rdf] {

  val turtleWriter: RDFWriter[Rdf, Try, Turtle]
  val jsonldCompactedWriter: RDFWriter[Rdf, Try, JsonLdCompacted]

  import ops._
  import sparqlOps._
  import rdfStore.sparqlEngineSyntax._
  import rdfStore.transactorSyntax._
  import rdfStore.sparqlUpdateSyntax._
  import rdfStore.transactorSyntax._
  import rdfStore.graphStoreSyntax._

  /**
   * sparql Construct Query;
   * NON transactional
   */
  def sparqlConstructQuery(queryString: String): Try[Rdf#Graph] = {
    val result = for {
      query <- {
        println("sparqlConstructQuery: before parseConstruct")
        parseConstruct(queryString)
      }
      es <- {
        println("sparqlConstructQuery: before executeConstruct")
        dataset.executeConstruct(query, Map())
      }
    } yield es
    result
  }

  /**
   * sparql Update Query;
   * NON transactional
   */
  def sparqlUpdateQuery(queryString: String, ds: DATASET = dataset): Try[Unit] = {
    val result = for {
      query <- {
        println("sparqlUpdateQuery: before parseUpdate")
        parseUpdate(queryString)
      }
      es <- {
        println("sparqlUpdateQuery: before executeUpdate")
        ds.executeUpdate(query, Map())
      }
    } yield es
    result
  }

  /** transactional, output Turtle String */
  def sparqlConstructQueryTR(queryString: String, format: String="turtle"): String = {
    val transaction = dataset.r({
      graph2String(sparqlConstructQuery(queryString), "", format)
    })
    transaction.get
  }

  /** transactional */
  def sparqlConstructQueryFuture(queryString: String): Future[Rdf#Graph] = {
    val r = sparqlConstructQuery(queryString)
    r.asFuture
  }

  //// special updates and queries ////

  /**
   * replace all triples having same subject and property
   * with given one, in given dataset;
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
    val res = sparqlUpdateQuery(queryString, dataset)
    println(s"replaceRDFnode: sparqlUpdateQuery: $res")

    rdfStore.appendToGraph(dataset, graphURI, makeGraph(Seq(triple)))
  }

  /**
   * remove quads whose subject is given URI
   *  No Transaction
   */
  def removeQuadsWithSubject(uri: Rdf#Node, ds: DATASET = dataset) = {
    val queryString = s"""
         | DELETE {
         |   graph ?graphURI {
         |     <$uri> ?property ?obj .
         |   }
         | } WHERE {
         |   graph ?graphURI {
         |     <$uri> ?property ?obj .
         |   }
         | }""".stripMargin
    sparqlUpdateQuery(queryString, ds)
  }

  /** a triple plus its named graph (empty URI if default graph) */
  type Quad = (Rdf#Triple, Rdf#URI)
  def quadQuery(s: Rdf#NodeMatch, p: Rdf#NodeMatch, o: Rdf#NodeMatch): Iterable[Quad] = {
    def makeSPARQLTermFromNodeMatch(nm: Rdf#NodeMatch, varName: String) = {
      foldNodeMatch(nm)(
        "?" + varName,
        node => makeTurtleTerm(node)
        /* TODO from an Rdf#Node, print the turtle term; betehess 15:22
         * @jmvanel nothing giving you that out-of-the-box right now
         * I'd write a new typeclass to handle that
         * it's super easy to do */
      )
    }
    def makeURI(node:Rdf#Node) = foldNode(node)(u=>u, b=>URI(""), l=>URI(""))
    def makeQuad(result: Seq[Rdf#Node]): Quad = {
      var resultIndex = 0
      val triple = Triple(
          foldNodeMatch(s)(
          {resultIndex += 1 ; result(resultIndex)},
          node => node ),
      foldNodeMatch(p)(
          {resultIndex += 1 ; makeURI( result(resultIndex) )},
          node => makeURI(node) ),
      foldNodeMatch(o)(
          {resultIndex += 1 ; result(resultIndex)},
          node => node )
      )
      ( triple, makeURI(result(resultIndex)) )
    }
    val variables0 = List(
      makeSPARQLTermFromNodeMatch(s, "S"),
      makeSPARQLTermFromNodeMatch(p, "P"),
      makeSPARQLTermFromNodeMatch(o, "O"))
    val variables = variables0 filter (s => s startsWith "?")

    val queryString = s"""
         | SELECT
         |     ${variables.mkString(" ")}
         |     ?G
         | WHERE {
         |   graph ?G {
         |     ${variables0.mkString(" ")}
         |     .
         |   }
         | }""".stripMargin
    println( "quadQuery " + queryString ) 
    val selectRes = sparqlSelectQueryVariablesNT(queryString, variables, dataset)
    selectRes map { makeQuad( _ ) }
  }

  //////////////// SELECT stuff //////////////////////////

  /** run SPARQL on given dataset, knowing result variables; transactional */
  def sparqlSelectQueryVariables(queryString: String, variables: Seq[String],
                                 ds: DATASET = dataset): List[Seq[Rdf#Node]] = {
    val transaction = ds.r({
      sparqlSelectQueryVariablesNT(queryString, variables, ds)
    })
    transaction.get
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

  /**
   * run SPARQL on given dataset; transactional
   * TODO the columns order may be wrong
   */
  def sparqlSelectQuery(queryString: String,
                        ds: DATASET = dataset): Try[List[List[Rdf#Node]]] = {
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
          println("after results")
          results.to[List]
      }
      println("before res")
      res
      //      println( "after res" )
    })
    println("before transaction.get")
    transaction.get
  }

  /** run SPARQL on given graph, knowing result variables */
  def runSparqlSelect(
    queryString: String, variables: Seq[String],
    graph: Rdf#Graph): List[Seq[Rdf#URI]] = {

    val query = parseSelect(queryString).get
    val answers: Rdf#Solutions = sparqlGraph.executeSelect(graph, query,
      Map()).get
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

  def graph2String(triples: Try[Rdf#Graph], baseURI: String, format: String="turtle"): String = {
    Logger.getRootLogger().info(s"base URI $baseURI ${triples}")
    val writer = if(format=="jsonld") jsonldCompactedWriter else turtleWriter // later add RDF/XML ......     
    val ret = writer.asString(triples.get, base = baseURI)
    ret.get
  }

  def dumpGraph(implicit graph: Rdf#Graph) = {
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