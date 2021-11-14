package deductions.runtime.sparql_cache

import java.io.ByteArrayOutputStream

import deductions.runtime.utils.RDFStoreLocalProvider
import deductions.runtime.utils.RDFOPerationsDB

import deductions.runtime.utils._
import org.w3.banana.io.{JsonLdCompacted, RDFWriter, RDFXML, Turtle}
import org.w3.banana.{RDF, TryW}
import play.api.libs.json.{JsArray, JsString, Json}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}
import org.w3.banana.RDFOps

import scalaz._
import Scalaz._

/** SPARQL Helpers without inheritance to RDFStoreLocalProvider,
 *  contrary to original SPARQLHelpers3;
 *  this is replaced by implicit arguments;
 *  so this trait is usable in classes without instantiation of RDFStoreLocalProvider
 *
 * TODO separate stuff depending on dataset, and stuff taking a graph in argument
 * @author jmv
 */
trait SPARQLHelpers3[Rdf <: RDF, DATASET]
    extends RDFOPerationsDB[Rdf, DATASET]
    with RDFHelpers0[Rdf]
    with RDFPrefixes[Rdf]
    with Timer {

  val config: Configuration
  
  val turtleWriter: RDFWriter[Rdf, Try, Turtle]
  val jsonldCompactedWriter: RDFWriter[Rdf, Try, JsonLdCompacted]
  val rdfXMLWriter: RDFWriter[Rdf, Try, RDFXML]

  implicit val ops: RDFOps[Rdf]
  import ops._

  import rdfStore.sparqlEngineSyntax._
  import rdfStore.sparqlUpdateSyntax._
  import rdfStore.transactorSyntax._
  import sparqlOps._

  /**
   * sparql Construct Query;
   * used in SPARQL results Web page
   * NEEDS transaction
   */
  def sparqlConstructQuery(queryString: String)
  (implicit rdfLocalProvider: RDFStoreLocalProvider[Rdf, DATASET])
  : Try[Rdf#Graph] = {
    import rdfLocalProvider.rdfStore.sparqlEngineSyntax._
    import sparqlOps._

    val result = for {
      query <- {
        logger.debug("sparqlConstructQuery: before parseConstruct")
        parseConstruct(queryString)
      }
//      _ = println( s"sparqlConstructQuery: query $query" )
      es <- {
        logger.debug("sparqlConstructQuery: before executeConstruct")
        rdfLocalProvider.dataset.executeConstruct(query, Map())
      }
    } yield es
    result
  }

  /**
   * sparql Construct Query;
   * With transaction
   */
  def sparqlConstructQueryGraph(queryString: String)
  (implicit rdfLocalProvider: RDFStoreLocalProvider[Rdf, DATASET])
  : Try[Rdf#Graph] =
    wrapInReadTransaction(sparqlConstructQuery(queryString)).flatten

  /**
   * sparql Update Query;
   * NON transactional
   */
  def sparqlUpdateQuery(queryString: String, dts: Option[DATASET])
   (implicit rdfLocalProvider: RDFStoreLocalProvider[Rdf, DATASET])
   : Try[Unit] = {
    val ds = getDataset(dts)
    val result = for {
      query <- {
        //        logger.debug(s"sparqlUpdateQuery: before parseUpdate $queryString")
        parseUpdate(queryString)
      }
      es <- {
        //        logger.debug("sparqlUpdateQuery: before executeUpdate")
        val result = ds.executeUpdate(query, Map())
        logger.debug(s"sparqlUpdateQuery: AFTER executeUpdate : $result")
        result
      }
    } yield es
    result
  }

  private def getDataset(dts: Option[DATASET])
  (implicit rdfLocalProvider: RDFStoreLocalProvider[Rdf, DATASET])
  =
  dts match {
      case Some(dataset) => dataset
      case None => rdfLocalProvider.dataset
    }
  
  /** wrap In RW Transaction */
  def wrapInTransaction[T](sourceCode: => T, dts: Option[DATASET])
  (implicit rdfLocalProvider: RDFStoreLocalProvider[Rdf, DATASET])
  = {
    val ds = getDataset(dts)
    val transaction = rdfStore.rw(ds, {
      sourceCode
    })
    transaction
  }

  /** wrap In Read Transaction */
  def wrapInReadTransaction[T](sourceCode: => T)
   (implicit rdfLocalProvider: RDFStoreLocalProvider[Rdf, DATASET])
   = {
    import rdfLocalProvider._
    val transaction = rdfLocalProvider.rdfStore.r(dataset, {
      sourceCode
    })
    transaction
  }

  /** transactional */
  def sparqlUpdateQueryTR(queryString: String, dts: Option[DATASET])
   (implicit rdfLocalProvider: RDFStoreLocalProvider[Rdf, DATASET])
  = {
    wrapInTransaction(sparqlUpdateQuery(queryString, dts), dts).flatten
  }

  /**
   * transactional, output Turtle String
   *  @param format = "turtle" or "rdfxml" or "jsonld"
   */
  def sparqlConstructQueryTR(queryString: String, format: String = "turtle")
    (implicit rdfLocalProvider: RDFStoreLocalProvider[Rdf, DATASET])
    :  Try[String] = {
    val transaction = rdfStore.r(rdfLocalProvider.dataset, {
      graph2String(sparqlConstructQuery(queryString), "", format)
    })
    transaction // .get
  }

  /** transactional */
  def sparqlConstructQueryFuture(queryString: String)
    (implicit rdfLocalProvider: RDFStoreLocalProvider[Rdf, DATASET])
  : Future[Rdf#Graph] = {
    val r = sparqlConstructQuery(queryString)
    r.asFuture
  }

  //// special updates and queries ////

  /**
   * replace all triples having same subject and property
   * with given one, in given dataset;
   *  thus enforcing cardinality one;
   *  NEEDS Transaction;
   *  See also [[deductions.runtime.sparql_cache.dataset.DatasetHelper#replaceObjects]]
   */
  def replaceRDFTriple(triple: Rdf#Triple, graphURI: Rdf#Node, dataset: DATASET)
  (implicit rdfLocalProvider: RDFStoreLocalProvider[Rdf, DATASET])
   = {
    val uri = triple.subject
    val property = triple.predicate

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
    logger.debug(s"""replaceRDFTriple: $triple in <$graphURI> """)
    val result = sparqlUpdateQuery(queryString, Some(dataset))(rdfLocalProvider)

    rdfStore.appendToGraph(dataset, nodeToURI(graphURI), makeGraph(Seq(triple)))
  }

  /** remove all triples having same subject and property in any named graph,
   *  and re-create given triple in named graph having a such triple */
  def replaceRDFTripleAnyGraph(triple: Rdf#Triple, dataset: DATASET)
    (implicit rdfLocalProvider: RDFStoreLocalProvider[Rdf, DATASET])
  = {
    val uri = triple.subject
    val property = triple.predicate
    val objet = triple.objectt

    val queryString = s"""
         | DELETE {
         |   graph ?GR {
         |     <$uri> <$property> ?ts .
         |   }}
         | INSERT {
         |   graph ?GR {
         |     <$uri> <$property> ${makeTurtleTerm(objet)} .
         | }}
         | WHERE {
         |   graph ?GR {
         |     <$uri> <$property> ?ts .
         |   }
         | }""".stripMargin
    logger.debug(s"""replaceRDFTripleAnyGraph: $triple""")
    val result = sparqlUpdateQuery(queryString, Some(dataset))
  }

  def getRDFList(subject: String, dts: Option[DATASET])
    (implicit rdfLocalProvider: RDFStoreLocalProvider[Rdf, DATASET])
  : List[Rdf#Node] = {
    val queryRdfList = s"""
    ${declarePrefix(rdf)}
    SELECT ?ELEM
    WHERE { GRAPH ?G {
      <SUBJECT> rdf:rest* / rdf:first ?ELEM
   } }
  """
    val q = queryRdfList.replace("<SUBJECT>", s"$subject")
    //    logger.debug(s"getRDFList: q $q")
    val res: List[Seq[Rdf#Node]] = sparqlSelectQueryVariablesNT(q, List("ELEM"), dts)
    //    logger.debug(s"getRDFList: res $res")
    res.flatten
  }

  /**
   * remove quads whose subject is given URI
   *  No Transaction
   */
  def removeQuadsWithSubject(uri: Rdf#Node, dts: Option[DATASET])
    (implicit rdfLocalProvider: RDFStoreLocalProvider[Rdf, DATASET])
  = {
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
    //    logger.debug( s"removeQuadsWithSubject $uri " + sparqlSelectQuery( queryString1 ) )

    //    logger.debug( s"removeQuadsWithSubject size() $size()" )
    val res = sparqlUpdateQuery(queryString, dts)
    logger.debug(s"removeQuadsWithSubject res ${res}")
  }

  /**
   * remove quads whose object is given URI
   *  No Transaction
   */
  def removeQuadsWithObject(objet: Rdf#Node, dts: Option[DATASET])
    (implicit rdfLocalProvider: RDFStoreLocalProvider[Rdf, DATASET])
  = {
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
    val res = sparqlUpdateQuery(queryString, dts)
  }

  /**
   * remove triples matching SPO Query, in any named graph;
   *  @return removed Quads
   *  DOES NOT include transaction
   */
  def removeFromQuadQuery(s: Rdf#NodeMatch, p: Rdf#NodeMatch, o: Rdf#NodeMatch)
    (implicit rdfLocalProvider: RDFStoreLocalProvider[Rdf, DATASET])
    : List[Quad] = {
    val quads = quadQuery(s, p, o).toList // : Iterable[Quad]
    //    logger.debug(s"removeFromQuadQuery: from $s $p $o: triples To remove $quads")
    quads.map {
      tripleToRemove =>
        rdfStore.removeTriples(rdfLocalProvider.dataset, tripleToRemove._2,
          List(tripleToRemove._1))
    }
    quads
  }

  /** a triple plus its named graph (empty URI if default graph) */
  type Quad = (Rdf#Triple, Rdf#URI)

  /* An SPO Query returning quads */
  def quadQuery(s: Rdf#NodeMatch, p: Rdf#NodeMatch, o: Rdf#NodeMatch)
  (implicit rdfLocalProvider: RDFStoreLocalProvider[Rdf, DATASET])
  : Iterable[Quad] = {

    def makeSPARQLTermFromNodeMatch(nm: Rdf#NodeMatch, varName: String) = {
      foldNodeMatch(nm)(
        "?" + varName,
        node => makeTurtleTerm(node))
    }
    def makeURI(node: Rdf#Node) = foldNode(node)(u => u, b => URI(""), l => URI(""))
    def makeQuad(result: Seq[Rdf#Node]): Quad = {
      var resultIndex = 0
      def processNodeMatch(nodeMatch: Rdf#NodeMatch): Rdf#Node = {
        //    	  logger.debug(s"processNodeMatch BEFORE result $result , resultIndex $resultIndex , nodeMatch $nodeMatch" )
        val res = foldNodeMatch(nodeMatch)(
          {
            val node = result(resultIndex)
            resultIndex += 1
            node
          },
          node => node)
        //          logger.debug(s"processNodeMatch result $result nodeMatch $nodeMatch" )
        res
      }
      val triple = Triple(
        processNodeMatch(s),
        makeURI(processNodeMatch(p)),
        processNodeMatch(o))
      //      logger.debug(s"processNodeMatch BEFORE makeURI(result(resultIndex)) , resultIndex $resultIndex size ${result.size}" )
      (triple, makeURI(result(resultIndex)))
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
    //    logger.debug( s"sparqlTerms $sparqlTerms" )
    //    logger.debug( s"variables $variables" )
    //    logger.debug( "quadQuery " + queryString ) 
    val selectRes = sparqlSelectQueryVariablesNT(queryString, variables, Some(rdfLocalProvider.dataset))
    //    logger.debug( s"selectRes $selectRes" )
    selectRes map { makeQuad(_) }
  }

  //////////////// SELECT stuff //////////////////////////

  /** run SPARQL on given dataset, knowing result variables; transactional */
  def sparqlSelectQueryVariables(queryString: String, variables: Seq[String],
                                 dts: Option[DATASET])
  (implicit rdfLocalProvider: RDFStoreLocalProvider[Rdf, DATASET])
  : List[Seq[Rdf#Node]] = {
    val ds = getDataset(dts)
    logger.debug("RRRRRRRRRR sparqlSelectQueryVariables before transaction")
    val transaction = rdfStore.r( ds, {
//    val transaction = ds.r({
      sparqlSelectQueryVariablesNT(queryString, variables, Some(ds))
    })
    logger.debug("RRRRRRRRRR sparqlSelectQueryVariables after transaction")
    transaction.get
  }

  /** run SPARQL on given dataset, knowing result variables; NOT transactional */
  def sparqlSelectQueryVariablesNT(queryString: String, variables: Seq[String],
                                   dts: Option[DATASET])
  (implicit rdfLocalProvider: RDFStoreLocalProvider[Rdf, DATASET])
  : List[Seq[Rdf#Node]] = {
    val ds = getDataset(dts)
    time("sparqlSelectQueryVariablesNT", {

      val solutionsTry = for {
        query <- parseSelect(queryString)
        es <- ds.executeSelect(query, Map())
      } yield es
      //      logger.debug( s"sparqlSelectQueryVariablesNT: $solutionsTry" )
      val solutionsTry2 = solutionsTry

      solutionsTry2 match {
        case Success(solutions) =>
          //    logger.debug( "solutionsTry.isSuccess " + solutionsTry.isSuccess )
          val answers: Rdf#Solutions = solutions
          // TODO nullPointer on empty database
          val results = answers.iterator.toIterable.map {
            row =>
              for (variable <- variables) yield {
                val cell = row(variable)
                cell match {
                  case Success(node) => node
                  case Failure(f)    => Literal(">>>> Failure: " + f.toString())
                }
              }
          }
          results.toList.
            // hack :(((((((((
            filter(node => !node.toString().contains(""">>>> Failure: """))

        case Failure(failure: org.apache.jena.query.QueryParseException) =>
          logger.error(s"sparqlSelectQueryVariablesNT: QueryParseException: $failure, queryString: $queryString")
          List(Seq(
            Literal(failure.getLocalizedMessage),
            Literal(queryString)))
        case Failure(failure) =>
          logger.error(s"sparqlSelectQueryVariablesNT: QueryParseException: $failure, queryString: $queryString")
          List(Seq(Literal(failure.getLocalizedMessage)))
      }
    }, isDebugEnabled(logger) )
  }

  /**
   * run SPARQL on given dataset; transactional;
   * the first row is the variables' list
   *  used in SPARQL results Web page
   */
  def sparqlSelectQuery(queryString: String,
                        dts: Option[DATASET])
  (implicit rdfLocalProvider: RDFStoreLocalProvider[Rdf, DATASET])
  : Try[List[Iterable[Rdf#Node]]] = {
    val ds = getDataset(dts)
    // DEBUG
    val dsg = ds.asInstanceOf[org.apache.jena.sparql.core.DatasetImpl].asDatasetGraph()
    println(s">>>> sparqlSelectQuery: dsg class : ${dsg.getClass}")
    println(s">>>> sparqlSelectQuery: ds: ${ds}")

    val transaction = rdfLocalProvider.rdfStore.r( ds, {
//    val transaction = ds.r({
      val solutionsTry = for {
        query <- parseSelect(queryString)
        es <- ds.executeSelect(query, Map())
      } yield es
      makeListofListsFromSolutions(solutionsTry)
    })
    logger.debug("sparqlSelectQuery: before transaction.get")
    transaction.get
  }

  /**
   * run SPARQL SELECT on given dataset; transactional;
   * the first row is the variables' list
   *  used in trait InstanceLabelsFromLabelProperty
   */
  def sparqlSelectQueryCompiled(query: Rdf#SelectQuery,
                        dts: Option[DATASET])
  (implicit rdfLocalProvider: RDFStoreLocalProvider[Rdf, DATASET])
  : Try[List[Iterable[Rdf#Node]]] = {
		val ds = getDataset(dts)
    val transaction = rdfStore.r( ds, {
//    val transaction = ds.r({
      val solutionsTry = for {
        es <- ds.executeSelect(query, Map())
      } yield es
      makeListofListsFromSolutions(solutionsTry)
    })
    logger.debug("sparqlSelectQuery: before transaction.get")
    transaction.get
  }

  /** @param addHeaderRow the first row is the variables' list */
  def makeListofListsFromSolutions(solutionsTry: Try[Rdf#Solutions],
                                   addHeaderRow: Boolean = true): Try[List[Iterable[Rdf#Node]]] = {
    import scala.collection._
    val res = solutionsTry.map {
      solutions =>
        val solsIterable = solutions.iterator.toIterable

//        val columnsMap2: scala.collection.mutable.SortedSet[String] = scala.collection.mutable.SortedSet()
//        val columnsMap2: scala.collection.mutable.Set[String] = scala.collection.mutable.Set()
        val columnsMap2: mutable.TreeSet[String] = mutable.TreeSet()
        solsIterable foreach {
          row =>
            val variables = row.varnames().toList
            columnsMap2 ++= variables
        }
//        println(s"columnsMap2 $columnsMap2")

        val results: Iterable[Iterable[Rdf#Node]] = solsIterable . map {
          row =>
            val rowSeq: mutable.Buffer[Rdf#Node] = mutable.Buffer()
            for (variable <- columnsMap2) {
              val n = row(variable).getOrElse(Literal("") )
              rowSeq += n
            }
//            println(s"rowSeq $rowSeq")
            rowSeq
        }
        logger.debug("sparqlSelectQuery: after results")

        if (addHeaderRow) {
//          val columnsCount = columnsMap.keys.max
//          val actualColumnsList = columnsMap(columnsCount)

          implicit val literalIsOrdered: scala.Ordering[Rdf#Literal] =
            scala.Ordering.by(lit => fromLiteral(lit)._1 )
          val r = columnsMap2 .map {
        	  name =>
        	  println(s"name $name")
        	  Literal(name)
          }
          val headerRow = r . toList // . sorted
     		  println(s"headerRow $headerRow ")

          val rrr = headerRow :: results.toList
          rrr

        } else results.toList
    }
    logger.debug("makeListofListsFromSolutions: before res")
    res
  }

  /**
   * sparql Select with "content negotiation" (conneg)
   * @param format = "turtle" or "rdfxml" or "jsonld"
   */
  def sparqlSelectConneg(queryString: String,
                         format: String = "xml",
                         dts: Option[DATASET])
  (implicit rdfLocalProvider: RDFStoreLocalProvider[Rdf, DATASET])
  : String = {
    val ds = getDataset(dts)
    logger.debug(s"format $format")
    format match {
      case "jsonld" => sparqlSelectJSON(queryString, dts)
      case "turtle" => sparqlSelectJSON(queryString, dts) // ??? should not happen
      case "rdfxml" => sparqlSelectXML(queryString, dts) // ??? should not happen
      case "xml"    => sparqlSelectXML(queryString, dts)
      case "json"   => sparqlSelectJSON(queryString, dts)
      case _        => sparqlSelectJSON(queryString, dts)
    }
  }

  /**
   * sparql Select, JSON output, see https://www.w3.org/TR/rdf-sparql-XMLres/#examples
   */
  def sparqlSelectXML(queryString: String,
                      dts: Option[DATASET])
  (implicit rdfLocalProvider: RDFStoreLocalProvider[Rdf, DATASET])
  : String = {
//		val ds = getDataset(dts)
    val result = sparqlSelectQuery(queryString, dts)
    val output = result match {
      case Success(res) =>
        if (!res.isEmpty) {
          val header = res.head.map { node => literalNodeToString(node) }
          logger.debug(s"sparqlSelectXML: header $header")

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
                      // logger.debug(s"sparqlSelectXML: listOfElements $listOfElements")
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
        } else {
          val xml =
            <sparql xmlns="http://www.w3.org/2005/sparql-results#">
              <head></head>
              <results></results>
            </sparql>
          xml
        }
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
                       dts: Option[DATASET])
  (implicit rdfLocalProvider: RDFStoreLocalProvider[Rdf, DATASET])
  : String = {
    val result = sparqlSelectQuery(queryString, dts)
    val output = result match {
      case Success(res) =>
        if (!res.isEmpty) {
          val header = res.head.map { node => literalNodeToString(node) }
          logger.debug(s"sparqlSelectJSON: header $header")
          val headValue = Json.obj("vars" -> JsArray(header.toSeq.map { s => JsString(s) }))
          val bindings = res.drop(1).map {
            list =>
              val listOfJSONobjects = list.map {
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
                        "xml:lang" -> {
                        litTuple._3 match {
                          case Some(lang) => lang.toString()
                          case None => ""
                        }}
                        )
                    })
              }
              val v = header.zip(listOfJSONobjects).map {
                pair => Json.obj(pair._1 -> pair._2)
              }
              val binding = v.fold(Json.obj())((a, b) => a ++ b)
              binding
          }
          val resultsValue = Json.obj("bindings" -> bindings)
          Json.obj(
            "head" -> headValue,
            "results" -> resultsValue)
        } else
          Json.obj("head" -> "", "results" -> "" )
      case Failure(f) => Json.toJson(f.getLocalizedMessage)
    }
    Json.prettyPrint(output)
  }

  /**
   * run SPARQL on given graph, knowing result variables
   * CAUTION: only URI's as results
   */
  def runSparqlSelect(
    queryString: String, variables: Seq[String],
    graph: Rdf#Graph): List[Seq[Rdf#URI]] = {

    val query = parseSelect(queryString).get
    val answers: Rdf#Solutions = sparqlGraph.executeSelect(graph, query,
      Map()).get
    val results: Iterator[Seq[Rdf#URI]] = answers.toIterable map {
      row =>
        //        logger.debug(s"row $row")
        for (variable <- variables) yield {
          val cell = row(variable)
          cell match {
            case Success(node)  => foldNode(node)(uri => uri, x => URI(""), x => URI(""))
            case Failure(error) => URI("")
          }
        }
    }
    //  TODO  val r = runSparqlSelectNodes( queryString, variables, graph)
    results.toList
  }

  /** run SPARQL on given graph, knowing result variables */
  def runSparqlSelectNodes(
    queryString: String, variables: Seq[String],
    graph: Rdf#Graph): List[Seq[Rdf#Node]] = {

    val q = parseSelect(queryString)
    if( q.isFailure )
    	System.err.println(s"runSparqlSelectNodes $q")
    val query = q.get
    val a = sparqlGraph.executeSelect(graph, query, Map())
    println(s"runSparqlSelectNodes answers $a")
    val answers = a.get
//    println(s"runSparqlSelectNodes answers size ${answers.toIterable.size}")

    val results = answers.toIterable map {
      row =>
        val varnames = row.varnames()
        logger.debug(s"row $row varnames $varnames")
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
          //          logger.debug (s", cell $cell")
          cell
        }
    }
    results.toList
  }

  def futureGraph2String(triples: Future[Rdf#Graph], uri: String): String = {
    val graph = Await.result(triples, 5000 millis)
    logger.info(s"uri $uri ${graph}")
    val to = new ByteArrayOutputStream
    val ret = turtleWriter.write(graph, to, base = Some(uri) )
    to.toString
  }

  /**
   * RDF graph to String
   *  @param format "turtle" or "rdfxml" or "jsonld"
   */
  def graph2String(triples: Try[Rdf#Graph], baseURI: String, format: String = "turtle"): String = {
    logger.info(s"graph2String: base URI <$baseURI>, format $format, ${triples}")
    triples match {
      case Success(graph) =>
        val graphSize = graph.size(ops)
        val (writer, stats) =
          if (format === "jsonld")
            (jsonldCompactedWriter, "")
          else if (format === "rdfxml")
            (rdfXMLWriter, s"<!-- graph size ${graphSize} -->\n")
          else
            (turtleWriter, s"# graph size ${graphSize}\n")

//        println( s">>>> graph2String baseURI $baseURI, graph $graph" )

        stats + {
          val tryString = writer.asString(graph, base = None) // baseURI)
//        println( s">>>> graph2String tryString $tryString" )
          tryString match {
            case Success(s) => s
            case Failure(f) => s"graph2String: trouble in writing graph: $f"
          }
        }
      case Failure(f) => f.getLocalizedMessage
    }
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

  def info(s: String) = logger.info(s)

}


