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
import org.w3.banana.syntax.NodeMatchSyntax

/** TODO separate stuff depending on dataset, and stuff taking a  graph in argument
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
//        println(s"sparqlUpdateQuery: before parseUpdate $queryString")
        parseUpdate(queryString)
      }
      es <- {
//        println("sparqlUpdateQuery: before executeUpdate")
        val r = ds.executeUpdate(query, Map())
//        println("sparqlUpdateQuery: AFTER executeUpdate")
        r
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
    println(s"replaceRDFnode: $triple in <$graphURI>")
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
         
    // DEBUG: same WHERE part
//    val queryString1 = s"""
//         | SELECT *
//         | } WHERE {
//         |   graph ?graphURI {
//         |     <$uri> ?property ?obj .
//         |   }
//         | }""".stripMargin
//    println( s"removeQuadsWithSubject $uri " + sparqlSelectQuery( queryString1 ) )
    
    val res = sparqlUpdateQuery(queryString, ds)
//    println( s"removeQuadsWithSubject res ${res}" )
  }

  /** remove triples matching SPO Query, in any named graph
   *  DOES NOT include transaction */
  def removeFromQuadQuery(s: Rdf#NodeMatch, p: Rdf#NodeMatch, o: Rdf#NodeMatch) = {
    val quads = quadQuery(s, p, o): Iterable[Quad]
//    println(s"triples To remove $quads")
    quads.map {
      tripleToRemove =>
        rdfStore.removeTriples(dataset, tripleToRemove._2,
          List(tripleToRemove._1))
    }
  }

  /** a triple plus its named graph (empty URI if default graph) */
  type Quad = (Rdf#Triple, Rdf#URI)

  /* An SPO Query returning quads */
  def quadQuery(s: Rdf#NodeMatch, p: Rdf#NodeMatch, o: Rdf#NodeMatch): Iterable[Quad] = {
    
    def makeSPARQLTermFromNodeMatch(nm: Rdf#NodeMatch, varName: String) = {
      foldNodeMatch(nm)(
        "?" + varName,
        node => makeTurtleTerm(node)
      )
    }
    def makeURI(node:Rdf#Node) = foldNode(node)(u=>u, b=>URI(""), l=>URI(""))
    def makeQuad(result: Seq[Rdf#Node]): Quad = {
      var resultIndex = 0
      def processNodeMatch(nodeMatch: Rdf#NodeMatch): Rdf#Node = {
//    	  println(s"processNodeMatch BEFORE result $result , resultIndex $resultIndex , nodeMatch $nodeMatch" )
        val res = foldNodeMatch(nodeMatch)(
          {
            val node = result(resultIndex)
            resultIndex += 1
            node
            },
          node => node )
//          println(s"processNodeMatch result $result nodeMatch $nodeMatch" )
        res
      }
      val triple = Triple(
        processNodeMatch(s),
        makeURI(processNodeMatch(p)),
        processNodeMatch(o))
//      println(s"processNodeMatch BEFORE makeURI(result(resultIndex)) , resultIndex $resultIndex size ${result.size}" )
      ( triple, makeURI(result(resultIndex)) )
    }

    val sparqlTerms = List(
      makeSPARQLTermFromNodeMatch(s, "S"),
      makeSPARQLTermFromNodeMatch(p, "P"),
      makeSPARQLTermFromNodeMatch(o, "O"))
    val v = sparqlTerms filter (s => s startsWith "?")
    val variables = v :+ "?G" // append ?G

    val queryString = s"""
         | SELECT
         |     ${variables.mkString(" ")}
         |     ?G
         | WHERE {
         |   graph ?G {
         |     ${sparqlTerms.mkString(" ")}
         |     .
         |   }
         | }""".stripMargin
//    println( s"sparqlTerms $sparqlTerms" )
//    println( s"variables $variables" )
//    println( "quadQuery " + queryString ) 
    val selectRes = sparqlSelectQueryVariablesNT(queryString, variables, dataset)
//    println( s"selectRes $selectRes" )
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

  /** run SPARQL on given graph, knowing result variables
   * CAUTION: only URI's as results */
  def runSparqlSelect(
    queryString: String, variables: Seq[String],
    graph: Rdf#Graph): List[Seq[Rdf#URI]] = {

    val query = parseSelect(queryString).get
    val answers: Rdf#Solutions = sparqlGraph.executeSelect(graph, query,
      Map()).get
    val results: Iterator[Seq[Rdf#URI]] = answers.toIterable map {
      row =>
        println(s"row $row")
        for (variable <- variables) yield {
          val cell = row(variable)
          cell match {
            case Success(node) => foldNode(node)( uri=> uri, x=>URI(""), x=>URI("") )
            case Failure(error) => URI("")
          }
        }
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
    Logger.getRootLogger().info(s"graph2String: base URI $baseURI ${triples}")
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
