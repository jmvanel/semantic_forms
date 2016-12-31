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
import org.w3.banana.io.JsonLdCompacted
import org.w3.banana.io.RDFWriter
import org.w3.banana.io.RDFXML
import org.w3.banana.io.Turtle

import deductions.runtime.dataset.RDFStoreLocalProvider
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.utils.RDFHelpers0
import deductions.runtime.utils.RDFPrefixes
import deductions.runtime.utils.Timer
import play.api.libs.json.JsArray
import play.api.libs.json.JsString
import play.api.libs.json.Json

/** TODO separate stuff depending on dataset, and stuff taking a  graph in argument
 * @author jmv
 */
trait SPARQLHelpers[Rdf <: RDF, DATASET]
    extends RDFStoreLocalProvider[Rdf, DATASET]
    		with RDFHelpers0[Rdf]
        with RDFPrefixes[Rdf]
    		with Timer {

	val config: Configuration
	
  val turtleWriter: RDFWriter[Rdf, Try, Turtle]
  val jsonldCompactedWriter: RDFWriter[Rdf, Try, JsonLdCompacted]
  val rdfXMLWriter: RDFWriter[Rdf, Try, RDFXML]

  import ops._
  import rdfStore.sparqlEngineSyntax._
  import rdfStore.sparqlUpdateSyntax._
  import rdfStore.transactorSyntax._
  import sparqlOps._

  /**
   * sparql Construct Query;
   * NEEDS transaction
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
        val result = ds.executeUpdate(query, Map())
        println(s"sparqlUpdateQuery: AFTER executeUpdate : $result")
        result
      }
    } yield es
    result
  }

  /** wrap In RW Transaction */
  def wrapInTransaction[T](sourceCode: => T) = {
    val transaction = dataset.rw({
      sourceCode
    })
    transaction
  }

  /** wrap In R Transaction */
  def wrapInReadTransaction[T](sourceCode: => T) = {
    val transaction = dataset.r({
      sourceCode
    })
    transaction
  }

  /** transactional */
  def sparqlUpdateQueryTR(queryString: String, ds: DATASET = dataset) =
    wrapInTransaction(sparqlUpdateQuery(queryString, ds)) . flatten

  /** transactional, output Turtle String
   *  @param format = "turtle" or "rdfxml" or "jsonld" */
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
   *  thus enforcing cardinality one;
   *  NEEDS Transaction;
   *  See also [[deductions.runtime.dataset.DatasetHelper#replaceObjects]]
   *  
   *  TODO remove all such triples in any named graph,
   *  and re-create given triple in first named graph having a such triple
   */
  def replaceRDFTriple(triple: Rdf#Triple, graphURI: Rdf#URI, dataset: DATASET) = {
    val uri = triple.subject
    val property = triple.predicate
    // TESTED
//    val queryString0 = s"""
//      WITH <$graphURI>
//      DELETE { <$uri> <$property> ?ts . }
//      # INSERT { <$uri> <$property> ??? . }
//      WHERE { <$uri> <$property> ?ts . }
//      """
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
    println(s"""replaceRDFTriple: $triple in <$graphURI> """)
    val result = sparqlUpdateQuery(queryString, dataset)
//    println(s"replaceRDFTriple: result: $result")

    rdfStore.appendToGraph(dataset, graphURI, makeGraph(Seq(triple)))
  }

  def getRDFList(subject: String): List[Rdf#Node] = {
    val queryRdfList = s"""
    ${declarePrefix(rdf)}
    SELECT ?ELEM
    WHERE { GRAPH ?G {
      <SUBJECT> rdf:rest* / rdf:first ?ELEM
   } }
  """
    val q = queryRdfList.replace("<SUBJECT>", s"$subject")
//    println(s"getRDFList: q $q")
    val res: List[Seq[Rdf#Node]] = sparqlSelectQueryVariablesNT(q, List("ELEM"))
//    println(s"getRDFList: res $res")
    res.flatten
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
    
//    println( s"removeQuadsWithSubject size() $size()" )
    val res = sparqlUpdateQuery(queryString, ds)
    println( s"removeQuadsWithSubject res ${res}" )
  }
  
    /**
   * remove quads whose object is given URI
   *  No Transaction
   */
  def removeQuadsWithObject(objet: Rdf#Node, ds: DATASET = dataset) = {
    val queryString = s"""
         | DELETE {
         |   GRAPH ?graphURI {
         |     ?subj ?property <$objet> .
         |   }
         | } WHERE {
         |   GRAPH ?graphURI {
         |     ?subj ?property <$objet> .
         |   }
         | }""".stripMargin
    val res = sparqlUpdateQuery(queryString, ds)
  }

  /** remove triples matching SPO Query, in any named graph;
   *  @return removed Quads
   *  DOES NOT include transaction */
  def removeFromQuadQuery(s: Rdf#NodeMatch, p: Rdf#NodeMatch, o: Rdf#NodeMatch): List[Quad] = {
    val quads = quadQuery(s, p, o).toList // : Iterable[Quad]
//    println(s"removeFromQuadQuery: from $s $p $o: triples To remove $quads")
    quads.map {
      tripleToRemove =>
        rdfStore.removeTriples(dataset, tripleToRemove._2,
          List(tripleToRemove._1))
    }
    quads
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
    println("RRRRRRRRRRRRRRRR sparqlSelectQueryVariables")
    val transaction = ds.r({
      sparqlSelectQueryVariablesNT(queryString, variables, ds)
    })
    println("RRRRRRRRRRRRRRRR")
    transaction.get
  }

  /** run SPARQL on given dataset, knowing result variables; NOT transactional */
  def sparqlSelectQueryVariablesNT(queryString: String, variables: Seq[String],
                                   ds: DATASET = dataset): List[Seq[Rdf#Node]] = {
    time("sparqlSelectQueryVariablesNT", {

      val solutionsTry = for {
        query <- Try(parseSelect(queryString))
        es <- Try(ds.executeSelect(query.get, Map()))
      } yield es
//      println( s"sparqlSelectQueryVariablesNT: $solutionsTry" )
      val solutionsTry2 = solutionsTry.flatten

      solutionsTry2 match {
        case Success(solutions) =>
          //    println( "solutionsTry.isSuccess " + solutionsTry.isSuccess )
          val answers: Rdf#Solutions = solutions
          val results = answers.iterator.toIterable.map {
            row =>
              for (variable <- variables) yield {
                val cell = row(variable)
                cell match {
                  case Success(node) => row(variable).get.as[Rdf#Node].get
                  case Failure(f)    => Literal(">>>> Failure: " + f.toString())
                }
              }
          }
          results.to[List].
            // hack :(((((((((
            filter(node => !node.toString().contains(""">>>> Failure: """))

        case Failure(failure: org.apache.jena.query.QueryParseException) =>
          println(s"sparqlSelectQueryVariablesNT: queryString: $queryString")
          List(Seq(
            Literal(failure.getLocalizedMessage),
            Literal(queryString)))
        case Failure(failure) => List(Seq(Literal(failure.getLocalizedMessage)))
      }

    },
      false)
  }

  /**
   * run SPARQL on given dataset; transactional;
   * the fisrt row is the variables' list
   *  used in SPARQL results Web page
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
          val solsIterable = solutions.iterator.toIterable
          val r = solsIterable . headOption . map {
            row =>
              val names = row.varnames().toList
              val headerRow = names . map {
                name => Literal(name)
              }
              headerRow
          }
          val headerRow = r.toList

          val results = solsIterable map {
            row =>
              val variables = row.varnames().toList
              for (variable <- variables) yield row(variable).get.as[Rdf#Node].get
          }
          println("after results")

          headerRow ++ results.to[List]
      }
      println("before res")
      res
      //      println( "after res" )
    })
    println("before transaction.get")
    transaction.get
  }

  /** sparql Select with "content negotiation" (conneg)
   *  @param format = "turtle" or "rdfxml" or "jsonld" */
  def sparqlSelectConneg(queryString: String,
      format: String="xml",
		  ds: DATASET = dataset): String = {
    println( s"format $format" )
			format match {
			  case "jsonld" => sparqlSelectJSON(queryString, ds)
			  case "turtle" => sparqlSelectJSON(queryString, ds) // should not happen
			  case "rdfxml" => sparqlSelectXML(queryString, ds) // should not happen
			  case "xml" => sparqlSelectXML(queryString, ds)
			  case "json" => sparqlSelectJSON(queryString, ds)
			  case _ => sparqlSelectJSON(queryString, ds)
			}
  }

  /**
   * sparql Select, JSON output, see https://www.w3.org/TR/rdf-sparql-XMLres/#examples
   */
  def sparqlSelectXML(queryString: String,
                      ds: DATASET = dataset): String = {
    val result = sparqlSelectQuery(queryString, ds)
    val output = result match {
      case Success(res) =>
        val header = res.head.map { node => literalNodeToString(node) }
        println(s"sparqlSelectXML: header $header")

        val xml =
          <sparql xmlns="http://www.w3.org/2005/sparql-results#">
            <head>
              { header.map { s => <variable name={ s }/> } }
            </head>
            <results>
              {
                val bindings = res.drop(1).map {
                  list =>
                    val listOfElements = list.map {
                      node =>
                        foldNode(node)(
                          uri => <uri>{ fromUri(uri) }</uri>,
                          bn => <bnode>{ fromBNode(bn) }</bnode>,
                          lit => {
                            val litTuple = fromLiteral(lit)
                            <literal xml:lang={ litTuple._3.toString() } datatype={ litTuple._2.toString() }>{ litTuple._1 }</literal>
                          })
                    }
                    // println(s"sparqlSelectXML: listOfElements $listOfElements")
                    val bindings = header.zip(listOfElements).map {
                      pair =>
                        <binding name={ pair._1 }>
                          { pair._2 }
                        </binding>
                    }
                    <result>
                      { bindings }
                    </result>
                }
                bindings
              }
            </results>
          </sparql>
        xml
      case Failure(f) => <sparql>
                           { f.getLocalizedMessage }
                         </sparql>
    }
//    output.toString()
    val printer = new scala.xml.PrettyPrinter(80, 2)
    printer.format(output)
  }
  
  /** sparql Select, JSON output, see https://www.w3.org/TR/sparql11-results-json/#example */
  def sparqlSelectJSON(queryString: String,
                       ds: DATASET = dataset): String = {
    val result = sparqlSelectQuery(queryString, ds)
    val output = result match {
      case Success(res) =>
        val header = res.head.map { node => literalNodeToString(node) }
        println( s"sparqlSelectJSON: header $header" )
        val headValue = Json.obj("vars" -> JsArray( header.map { s => JsString(s) } ) )
        val bindings = res.drop(1).map {
          list =>
            val listOfJSOjects = list.map {
              node =>
                foldNode(node)(
                  uri => Json.obj("type" -> "uri",
                    "value" -> fromUri(uri)),
                  bn => Json.obj("type" -> "bnode",
                    "value" -> fromBNode(bn)),
                  lit => {
                    val litTuple = fromLiteral(lit)
                    Json.obj(
                      "type" -> "literal",
                      "value" -> litTuple._1,
                      "datatype" -> litTuple._2.toString(),
                      "xml:lang" -> litTuple._3.toString())
                  })
            }
            val v = header.zip(listOfJSOjects).map {
              pair => Json.obj(pair._1 -> pair._2)
            }
            val binding = v.fold(Json.obj())((a, b) => a ++ b)
            binding
        }
        val resultsValue = Json.obj("bindings" -> bindings)
        Json.obj(
          "head" -> headValue,
          "results" -> resultsValue)
      case Failure(f) => Json.toJson( f.getLocalizedMessage )
    }
    Json.prettyPrint(output)
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
//        println(s"row $row")
        for (variable <- variables) yield {
          val cell = row(variable)
          cell match {
            case Success(node) => foldNode(node)( uri=> uri, x=>URI(""), x=>URI("") )
            case Failure(error) => URI("")
          }
        }
    }
//  TODO  val r = runSparqlSelectNodes( queryString, variables, graph)
    results.to[List]
  }

  /** run SPARQL on given graph, knowing result variables */
  def runSparqlSelectNodes(
    queryString: String, variables: Seq[String],
    graph: Rdf#Graph): List[Seq[Rdf#Node]] = {

    val query = parseSelect(queryString).get
    val answers: Rdf#Solutions = sparqlGraph.executeSelect(graph, query,
      Map()).get
    val results = answers.toIterable map {
      row =>
        val varnames = row.varnames()
//        if (varnames.contains("COMM")) 
//        println(s"row $row")
        //        val effectiveVariables = varnames.intersect(variables.toSet)
        for (variable <- variables) yield {
          val cell = if (varnames.contains(variable)) {
//        	  print(s"row variable $variable")
            val cell = row(variable)
            cell match {
              case Success(node)  => node
              case Failure(error) => Literal(error.getLocalizedMessage)
            }
          } else Literal("")
//          println (s", cell $cell")
          cell
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

  /** RDF graph to String
   *  @param format = "turtle" or "rdfxml" or "jsonld" */
  def graph2String(triples: Try[Rdf#Graph], baseURI: String, format: String = "turtle"): String = {
    Logger.getRootLogger().info(s"graph2String: base URI $baseURI ${triples}")
    val writer =
      if (format == "jsonld") jsonldCompactedWriter
      else if (format == "rdfxml") rdfXMLWriter
      else turtleWriter
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

object SPARQLHelper extends ImplementationSettings.RDFModule
    with ImplementationSettings.RDFCache
    with SPARQLHelpers[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {

	val config = new DefaultConfiguration {
    override val useTextQuery = false
  }
	import config._
	
  def selectJSON(queryString: String): String = {
    sparqlSelectJSON(queryString)
  }

    def selectXML(queryString: String): String = {
    sparqlSelectXML(queryString)
  }
//  def select(queryString: String): String = {
//    sparqlSelectQuery(queryString).toString()
//  }

}
