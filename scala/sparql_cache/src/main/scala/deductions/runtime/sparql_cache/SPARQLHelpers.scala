package deductions.runtime.sparql_cache

import java.io.ByteArrayOutputStream

import deductions.runtime.utils.RDFStoreLocalProvider
import deductions.runtime.utils._

import org.w3.banana.io.{JsonLdCompacted, RDFWriter, RDFXML, Turtle}
import org.w3.banana.{RDF, TryW}
import play.api.libs.json.{JsArray, JsString, Json}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}
import org.w3.banana.SparqlEngine
import scala.xml.Comment

import scalaz._
import Scalaz._
import org.w3.banana.OWLPrefix
import deductions.runtime.connectors.icalendar.RDF2ICalendar
import play.api.libs.json.JsNull
import deductions.runtime.utils.RDFContentNegociation
import org.w3.banana.io.NTriples
import scala.jdk.CollectionConverters._
import org.apache.jena.util.PrefixMappingUtils
import org.apache.jena.riot.RDFFormat
import org.apache.jena.rdf.model.ModelFactory
import deductions.runtime.core.HTTPrequest
import org.apache.jena.riot.RDFLanguages
import org.apache.jena.riot.RDFWriterRegistry

/**
 * TODO separate stuff depending on dataset, and stuff taking a graph in argument
 * @author jmv
 */
trait SPARQLHelpers[Rdf <: RDF, DATASET]
    extends RDFStoreLocalProvider[Rdf, DATASET]
    with RDFHelpers0[Rdf]
    with RDFPrefixes[Rdf]
    with RDF2ICalendar[Rdf, DATASET]
    with Timer
    with RDFContentNegociation 
    with SparqlSelectGeneric [Rdf, DATASET]
    with jmvanel.GeoJSONexportAPI {

  val config: Configuration

  val turtleWriter: RDFWriter[Rdf, Try, Turtle]
  val jsonldCompactedWriter: RDFWriter[Rdf, Try, JsonLdCompacted]
  val rdfXMLWriter: RDFWriter[Rdf, Try, RDFXML]
  val ntriplesWriter: RDFWriter[Rdf, Try, NTriples]

  import ops._
  import rdfStore.sparqlEngineSyntax._
  import rdfStore.sparqlUpdateSyntax._
  import rdfStore.transactorSyntax._
  import sparqlOps._

  val jenaComplements: SparqlComplements[Rdf, DATASET]
  val graphWriter: GraphWriterPrefixMapTrait[Rdf]

  /**
   * sparql Construct Query with Arq Syntax;
   * used in SPARQL results Web page
   * NEEDS transaction
   */
  private def sparqlConstructQueryArqSyntax(
		  queryString: String,
      bindings: Map[String, Rdf#Node] = Map(),
      context: Map[String,String] = Map()
      ): Try[DATASET] = {

    val result = for {
      es <- {
        logger.debug("sparqlConstructQueryArqSyntax: before executeConstruct")
        jenaComplements.executeConstructArqSyntax(dataset, queryString, bindings)
      }
    } yield {
      es
    }
    result
  }

  /**
   * sparql Construct Query with SPARQL 1.1 Syntax;
   * used in SPARQL results Web page
   * NEEDS transaction
   */
  def sparqlConstructQuery(
		  queryString0: String,
      bindings: Map[String, Rdf#Node] = Map(),
      context: Map[String,String] = Map()
      ): Try[Rdf#Graph] = {
    val queryString = sparqlEnsureLimit(queryString0)
    val result = for {
      query <- {
        logger.debug("sparqlConstructQuery: before parseConstruct")
        parseConstruct(queryString)
      }
      //      _ = logger.debug( s"sparqlConstructQuery: query $query" )
      es <- {
        logger.debug(s"sparqlConstructQuery: before executeConstruct, query $query")
        if (checkUnionDefaultGraph(context))
          jenaComplements.executeConstructUnionGraph(dataset, query: Rdf#ConstructQuery)
        else
          dataset.executeConstruct(query, bindings)
      }
    } yield {
      enrichGraphFromTDB(es, context)
    }
    result
  }

  /** SPARQL: Ensure presence of Limit in query */
  private def sparqlEnsureLimit(queryString: String): String = {
    if( queryString.contains("LIMIT ") )
        queryString
    else
      queryString + " LIMIT 5000"
  }

  /** enrich given Graph with computed labels From TDB */
  private def enrichGraphFromTDB(graph: Rdf#Graph, context: Map[String, String]) = {
//    println( "printNsPrefixMap(graph)" )
//    printNsPrefixMap(graph)
//    println( "printNsPrefixMap( allNamedGraph )")
//    printNsPrefixMap( allNamedGraph )
    // println( ">>>> enrichGraphFromTDB: dataset.isInstanceOf[org.apache.jena.query.Dataset]" + 
//       dataset.isInstanceOf[org.apache.jena.query.Dataset] )
    val dsJena = dataset.asInstanceOf[org.apache.jena.query.Dataset]
    // println( "getDefaultModel.getNsPrefixMap '" +
//        dsJena.getDefaultModel.getNsPrefixMap.asScala.mkString(", ") + "'")
    val jenaGraph = graph.asInstanceOf[org.apache.jena.graph.Graph]
//    println( "prefix2uriMap " + jenaGraph.getPrefixMapping.setNsPrefixes(prefix2uriMap.asJava) )
    if (context.get("enrich").isDefined) {
      /* loop on each unique subject ?S in graph:
		   * - compute instance label ?LABEL
		   * - add triple ?S rdfs:label ?LABEL */
      val subjects = for (
        tr <- graph.triples;
        sub = tr.subject
      ) yield sub
      val unionGraph = allNamedGraph
      val addedTriples: List[Rdf#Triple] = for (subject <- subjects.toList.distinct) yield {
        val lang = context.get("lang").getOrElse("en")
        val labelTriple = find( unionGraph, subject, URI("urn:displayLabel"), ANY) .
        toSeq.headOption . getOrElse(Triple(nullURI, nullURI, Literal("")))
        // urn:/semforms/labelsGraphUri/fr urn:displayLabel "Test texte"
        val instanceLabelFromTDB = nodeToString( labelTriple.objectt )
        Triple(subject, rdfs.label,
          Literal(instanceLabelFromTDB))
//          Literal.tagged(instanceLabelFromTDB, Lang(lang)))
      }
      logger.debug( s">>>> enrichGraphFromTDB: addedTriples: '$addedTriples'" )
      graph union (Graph(addedTriples))
    } else {
      logger.debug( s">>>> NO enrichGraphFromTDB" )
      graph
    }
  }

  private def printNsPrefixMap(graph: Rdf#Graph) = {
    val jenaGraph = graph.asInstanceOf[org.apache.jena.graph.Graph]
    val mapping = PrefixMappingUtils.calcInUsePrefixMapping(jenaGraph)
    val map = mapping.getNsPrefixMap();
    println(map.size())
    println(map.asScala.mkString(", "))
  }
  private def printNsPrefixMap0(graph: Rdf#Graph) = {
    if (graph.isInstanceOf[org.apache.jena.graph.Graph]) {
      println(">>>> graph .asInstanceOf[org.apache.jena.graph.Graph]")
      val jenaGraph = graph.asInstanceOf[org.apache.jena.graph.Graph]
      val mapping = jenaGraph.getPrefixMapping
      val map = mapping.getNsPrefixMap();
      println( map.size() )
      println( map.asScala.mkString(", "))
    }
  }

  private def checkUnionDefaultGraph(context: Map[String,String]) = {
    val okValues = Set("true", "on")
    okValues contains ( context.get("unionDefaultGraph").getOrElse("") )
  }
  val sparqlGraph: SparqlEngine[Rdf, Try, Rdf#Graph]

  /** */
  def sparqlConstructQueryFromGraph(queryString: String, graph: Rdf#Graph,
      bindings: Map[String, Rdf#Node] = Map() ): Try[Rdf#Graph] = {
    val result = for {
      query <- {
        logger.debug("sparqlConstructQuery: before parseConstruct")
        parseConstruct(queryString)
      }
//      _ = logger.debug( s"sparqlConstructQueryFromGraph: query $query" )
      es <- {
        logger.debug("sparqlConstructQueryFromGraph: before executeConstruct")
        sparqlGraph.executeConstruct(graph, query, bindings)
      }
    } yield es
    result
  }
    
  /**
   * sparql Construct Query;
   * With transaction
   */
  def sparqlConstructQueryGraph(
      queryString: String,
      context: Map[String,String] = Map()): Try[Rdf#Graph] =
    wrapInReadTransaction(sparqlConstructQuery(queryString, context=context)).flatten

  /**
   * sparql Update Query;
   * NON transactional
   */
  def sparqlUpdateQuery(queryString: String, ds: DATASET = dataset): Try[Unit] = {
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

  /** wrap In RW Transaction */
  def wrapInTransaction[T](sourceCode: => T, ds:DATASET=dataset) = {
    val transaction = rdfStore.rw(ds, {
      sourceCode
    })
    transaction
  }

  /** wrap In Read Transaction */
  def wrapInReadTransaction[T](sourceCode: => T) = {
    val transaction = rdfStore.r(dataset, {
      sourceCode
    })
    transaction
  }

  /** transactional */
  def sparqlUpdateQueryTR(queryString: String, ds: DATASET = dataset) =
    wrapInTransaction(sparqlUpdateQuery(queryString, ds)).flatten

  /**
   * transactional, output Turtle String
   *  @param format = "turtle" or "rdfxml" or "jsonld"
   *  with arg. request: HTTPrequest , TODO set base URI
   */
  def sparqlConstructQueryTR(queryString: String, request: HTTPrequest, format: String = "turtle",
                             context: Map[String, String] = Map()): Try[String] = {
      val transaction = rdfStore.r(dataset, {
        logger.debug( s"sparqlConstructQueryTR: query $queryString \n\t\tformat $format, context $context" )
        graph2String(
          sparqlConstructQuery(queryString, context=context), request,
          "", format)
    })
    transaction .flatten
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
   *  See also [[deductions.runtime.sparql_cache.dataset.DatasetHelper#replaceObjects]]
   */
  def replaceRDFTriple(triple: Rdf#Triple, graphURI: Rdf#Node, dataset: DATASET) = {
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
    val result = sparqlUpdateQuery(queryString, dataset)

    rdfStore.appendToGraph(dataset, nodeToURI(graphURI), makeGraph(Seq(triple)))
  }

  /** remove all triples having same subject and property in any named graph,
   *  and re-create given triple in named graph having a such triple */
  def replaceRDFTripleAnyGraph(triple: Rdf#Triple, dataset: DATASET) = {
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
    val result = sparqlUpdateQuery(queryString, dataset)
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
    //    logger.debug(s"getRDFList: q $q")
    val res: List[Seq[Rdf#Node]] = sparqlSelectQueryVariablesNT(q, List("ELEM"))
    //    logger.debug(s"getRDFList: res $res")
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
    //    logger.debug( s"removeQuadsWithSubject $uri " + sparqlSelectQuery( queryString1 ) )

    //    logger.debug( s"removeQuadsWithSubject size() $size()" )
    val res = sparqlUpdateQuery(queryString, ds)
    logger.debug(s"removeQuadsWithSubject res ${res}")
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

  /**
   * remove triples matching SPO Query, in any named graph;
   *  @return removed Quads
   *  DOES NOT include transaction
   */
  def removeFromQuadQuery(s: Rdf#NodeMatch, p: Rdf#NodeMatch, o: Rdf#NodeMatch): List[Quad] = {
    val quads = quadQuery(s, p, o).toList // : Iterable[Quad]
    //    logger.debug(s"removeFromQuadQuery: from $s $p $o: triples To remove $quads")
    quads.map {
      tripleToRemove =>
        rdfStore.removeTriples(dataset, tripleToRemove._2,
          List(tripleToRemove._1))
    }
    quads
  }

  /** a triple plus its named graph (empty URI if default graph) */
  type Quad = (Rdf#Triple, Rdf#URI)

  /** An SPO Query returning quads
   *  TODO, too complex, use Jena */
  def quadQuery(s: Rdf#NodeMatch, p: Rdf#NodeMatch, o: Rdf#NodeMatch): Iterable[Quad] = {

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
        logger.debug(s"""processNodeMatch BEFORE makeURI(result(resultIndex)) ,
        resultIndex $resultIndex size ${result.size}
        triple $triple""" )
      (triple, makeURI(result(resultIndex)))
    }

    val sparqlTerms = List(
      makeSPARQLTermFromNodeMatch(s, "S"),
      makeSPARQLTermFromNodeMatch(p, "P"),
      makeSPARQLTermFromNodeMatch(o, "O"))
    val v = sparqlTerms filter (s => s startsWith "?")
    val variables = v :+ "?G" // append ?G

    val queryString = s"""
         |# quadQuery($s, $p, $o)
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
    val selectRes = sparqlSelectQueryVariablesNT(queryString, variables, dataset)
    //    logger.debug( s"selectRes $selectRes" )
    selectRes map { makeQuad(_) }
  }

  //////////////// SELECT stuff //////////////////////////

  /** run SPARQL on given dataset, knowing result variables; transactional */
  def sparqlSelectQueryVariables(queryString: String, variables: Seq[String],
                                 ds: DATASET = dataset): List[Seq[Rdf#Node]] = {
    logger.debug("RRRRRRRRRR sparqlSelectQueryVariables before transaction")
    val transaction = rdfStore.r( ds, {
      sparqlSelectQueryVariablesNT(queryString, variables, ds)
    })
    logger.debug("RRRRRRRRRR sparqlSelectQueryVariables after transaction")
    transaction.get
  }

  /** run SPARQL on given dataset, knowing result variables;
   *  NOT transactional (needs to be called within a Read transaction)
      CAUTION: slow with /lookup and /search , why ??? */
  def sparqlSelectQueryVariablesNT(queryString: String, variables: Seq[String],
                                   ds: DATASET = dataset): List[Seq[Rdf#Node]] = {
//    if(queryString.contains("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>"))
//      println(s"sparqlSelectQueryVariablesNT( $queryString")

    val queryForLog = "'" + queryString
    .substring(0, Math.min(40, queryString.length() - 1))
    .replace( '\n', ' ')  + "'"
    val solutionsTry =
      time("sparqlSelectQueryVariablesNT: time: " + queryForLog, {
      for {
        query <- parseSelect(queryString)
        es <- ds.executeSelect(query, Map())
      } yield es
    }, isDebugEnabled(logger) )

    try {
    time("sparqlSelectQueryVariablesNT 2: " + queryForLog, {
      solutionsTry match {
        case Success(solutions) =>
          logger.debug( "sparqlSelectQueryVariablesNT: solutionsTry.isSuccess " + solutionsTry.isSuccess )
          // TODO nullPointer on empty database
          val results = solutions.iterator().iterator.to(Iterable).map {
            row =>
              // TODO rather loop on row.varnames() , to make sparqlSelectQueryVariablesNT more robust
               logger.debug( s"sparqlSelectQueryVariablesNT: row $row" )
               for (variable <- variables) yield {
                val cell = row(variable)
                cell match {
                  case Success(node) => node
                  case Failure(f)    => Literal(">>>> Failure: " + f.toString())
                }
              }
          }
          results.toList. // results.to[List].
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
    }, isDebugEnabled(logger)
    )
    }
    catch {
      case t: Throwable =>
        logger.error( s"ERROR in sparqlSelectQueryVariablesNT: ${t.getLocalizedMessage}" )
        t.printStackTrace()
        List(Seq())
    }
  }

  /**
   * run SPARQL on given dataset; transactional;
   * the first row is the variables' list
   *  used in SPARQL results Web page
   *  @param context used for setting UnionGraph
   */
  def sparqlSelectQuery(queryString: String,
                        ds: DATASET = dataset,
                        context: Map[String,String] = Map()
		  ): Try[List[Iterable[Rdf#Node]]] = {
    val dsg = ds.asInstanceOf[org.apache.jena.sparql.core.DatasetImpl].asDatasetGraph()
    logger.debug(s">>>> sparqlSelectQuery: dsg class : ${dsg.getClass}")
    logger.debug(s">>>> sparqlSelectQuery: ds: ${ds}")

    val transaction = rdfStore.r( ds, {
      val solutionsTry = for {
        query <- parseSelect(queryString)
        es <- {
          if (checkUnionDefaultGraph(context))
            jenaComplements.executeSelectUnionGraph(dataset, query, Map())
          else
            ds.executeSelect(query, Map())
        }
      } yield es
      // logger.info(s">>>> sparqlSelectQuery: solutionsTry ${solutionsTry.get.iterator().toIterator.mkString(", ") }")
      makeListofListsFromSolutions(solutionsTry)
    })
    logger.debug("sparqlSelectQuery: before transaction.get")
    if( transaction.isFailure )
      logger.warn(s"""sparqlSelectQuery Failure : ${transaction.failed}
        queryString
        $queryString""")
    transaction.get
  }

  /**
   * run SPARQL SELECT on given dataset;
   * transactional;
   * @param query : a compiled query
   * the first row is the variables' list
   *  used in trait InstanceLabelsFromLabelProperty
   */
  def sparqlSelectQueryCompiled(query: Rdf#SelectQuery,
                        ds: DATASET = dataset): Try[List[Iterable[Rdf#Node]]] = {
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

  /** make List of Lists From Solutions
   *  @param addHeaderRow the first row is the variables' list */
  def makeListofListsFromSolutions(
    solutionsTry: Try[Rdf#Solutions],
    addHeaderRow: Boolean            = true): Try[List[Iterable[Rdf#Node]]] = {

    import scala.collection._
    val res = solutionsTry.map {
      solutions =>
        /* CAUTION: an Iterator should be only in one loop,
         * cf https://www.scala-lang.org/api/current/scala/collection/Iterator.html */
        val solsIterable = solutions.iterator().toList
        val columnsMap2: mutable.TreeSet[String] = mutable.TreeSet()
        solsIterable foreach {
          row =>
            val variables = row.varnames().toList
            columnsMap2 ++= variables
        }
        val results = solsIterable . map {
          row =>
            val rowSeq: mutable.Buffer[Rdf#Node] = mutable.Buffer()
            for (variable <- columnsMap2) rowSeq +=
              row(variable).getOrElse(Literal(""))
            logger.debug(s"makeListofListsFromSolutions: rowSeq $rowSeq")
            rowSeq
        }
        logger.trace(s"makeListofListsFromSolutions: after results : results \n\t${results.mkString(",\n\t")}")

        if (addHeaderRow) {
          implicit val literalIsOrdered: scala.Ordering[Rdf#Literal] =
            scala.Ordering.by(lit => fromLiteral(lit)._1 )
          val r = columnsMap2 .map {
        	  name =>
        	  logger.debug(s"name $name")
        	  Literal(name)
          }
          val headerRow = r . toList // . sorted
          logger.debug(s"makeListofListsFromSolutions: headerRow $headerRow ")
          headerRow :: results.toList
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
                         ds: DATASET = dataset,
                         context: Map[String,String] = Map()): Try[String] = {
    logger.debug(s"format $format")
    format match {
      case "jsonld" => sparqlSelectJSON(queryString, ds, context)
      case "turtle" => sparqlSelectJSON(queryString, ds) // ??? should not happen
      case "rdfxml" => sparqlSelectXML(queryString, ds) // ??? should not happen
      case "xml"    => sparqlSelectXML(queryString, ds, context)
      case "json"   => sparqlSelectJSON(queryString, ds, context)
      case "csv"   => sparqlSelectWrite(queryString, ds, context)
      case _        => sparqlSelectJSON(queryString, ds)
    }
  }

  /**
   * sparql Select, JSON output, see https://www.w3.org/TR/rdf-sparql-XMLres/#examples
   */
  def sparqlSelectXML(queryString: String,
                      ds: DATASET = dataset,
                      context: Map[String,String] = Map()): Try[String] = {
    val result = sparqlSelectQuery(queryString, ds, context)
    val output = result match {
      case Success(res) =>
        val printer = new scala.xml.PrettyPrinter(1000, 2) // (80, 2)
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
                            uri =>
//                              { if(uri.toString.startsWith(
//                                  "http://raw.githubusercontent.com/jmvanel/semantic_forms/master/vocabulary/forms.owl.ttl"))
//                                println(s"sparqlSelectXML !!!!!!! <$uri> ")
                              <uri>{ fromUri(uri) }</uri>
//                                }
                              ,
                            bn => <bnode>{ fromBNode(bn) }</bnode>,
                            lit => {
                              val litTuple = fromLiteral(lit)
                              <literal xml:lang={
                                val lang = litTuple._3.toString()
                                if(lang == "") null else lang
                              } datatype={ litTuple._2.toString() }>{ litTuple._1 }</literal>
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
          Success( printer.format(xml) )
        } else {
          val xml =
            <sparql xmlns="http://www.w3.org/2005/sparql-results#">
              <head></head>
              <results></results>
            </sparql>
          Success( printer.format(xml) )
        }
      case Failure(f) => Failure(f)
//        <sparql>
//          {
//            Comment(
//              "ERROR in sparql Select:\n" +
//                f.getLocalizedMessage)
//          }
//        </sparql>
    }
    output
  }

  /** sparql Select, JSON output, see https://www.w3.org/TR/sparql11-results-json/#example */
  def sparqlSelectJSON(queryString: String,
                       ds: DATASET = dataset,
                       context: Map[String,String] = Map()): Try[String] = {
    val result = sparqlSelectQuery(queryString, ds, context)
//    println(s">>>> sparqlSelectJSON: queryString '$queryString', context $context, result $result")
    val output = result match {
      case Success(res) =>
        if (!res.isEmpty) {
          val header = res.head.map { node => literalNodeToString(node) }
          logger.debug(s"sparqlSelectJSON: header $header")
          val headValue = Json.obj("vars" -> JsArray(header.toSeq.map { s => JsString(s) }))
          val bindings = res.drop(1).map {
            list =>
//              println(s"list (row) ${list.mkString("; ")}")
              val listOfJSONobjects = list.map {
                node =>
                  foldNode(node)(
                    uri => Json.obj("type" -> "uri",
                      "value" -> fromUri(uri)),
                    bn => Json.obj("type" -> "bnode",
                      "value" -> fromBNode(bn)),
                    lit => {
                      if(lit != null) {
                      val litTuple = fromLiteral(lit)
//                      println(s"lit $lit")
//                      println(s"litTuple $litTuple")
                      if(litTuple._1 != "" )
                      Json.obj(
                        "type" -> "literal",
                        "value" -> litTuple._1,
                        "datatype" -> litTuple._2.toString(),
                        "xml:lang" -> {
                              if (litTuple._3 != null) {
                                litTuple._3 match {
                                  case Some(lang) => lang.toString()
                                  case None       => JsNull
                                }
                              } else {
                                logger.warn(s"sparqlSelectJSON: null in literal lang: ${list.mkString("; ")}")
                                JsNull
                              }
                        }
                        )
                        else Json.obj()
                    } else {
                      logger.warn(s"sparqlSelectJSON: null in RDF node: ${list.mkString("; ")}")
                      JsNull
                    }
                    })
              }
              val v = header.zip(listOfJSONobjects).map {
                pair => Json.obj(pair._1 -> pair._2)
              }
              val binding = v.fold(Json.obj())((a, b) => a ++ b)
              binding
          }
          val resultsValue = Json.obj("bindings" -> bindings)
          Success(
          Json.prettyPrint(
          Json.obj(
            "head" -> headValue,
            "results" -> resultsValue) ) )
        } else
          Success(
          Json.prettyPrint(
              Json.obj("head" -> "", "results" -> "" )))
      case Failure(f) => Failure(f)
        // Json.toJson(f.getLocalizedMessage)
    }
    output
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
    // results.to[List]
    results.toList
  }

  /** run SPARQL on given graph, knowing result variables */
  def runSparqlSelectNodes(
    queryString: String, variables: Seq[String],
    graph: Rdf#Graph): List[Seq[Rdf#Node]] = {

    val q = parseSelect(queryString)
    if( q.isFailure )
    	logger.error(s"runSparqlSelectNodes $q")
    val query = q.get
    val a = sparqlGraph.executeSelect(graph, query, Map())
    logger.trace(s"runSparqlSelectNodes answers $a")
    val answers = a.get
//    logger.debug(s"runSparqlSelectNodes answers size ${answers.toIterable.size}")

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
    val ret = turtleWriter.write(graph, to, base = Some(uri))
    to.toString
  }

  /**
   * RDF graph to String
   *  @param format "turtle" or "rdfxml" or "jsonld" or ntMime = "application/n-triples"
   *  TODO nothing to do with SPARQL; move to other trait called GraphFormatter
   *  TODO REFACTOR, use conneg helper RDFContentNegociation (use MIME types directly)
   *  then pass MIME type
   */
  def graph2String(triples: Try[Rdf#Graph],
      request: HTTPrequest,
      baseURI: String, format: String = "turtle"): Try[String] = {
    val formatFromURL = request.getHTTPparameterValue("format").getOrElse("")
    logger.debug( s""">>>> graph2String format from HTTP header: '$format'
      formatFromURL (priority) '$formatFromURL'
        $triples""" )
//        prefix2uriMap ${prefix2uriMap.mkString("\n")}""" )

    val outputStream = new ByteArrayOutputStream
    def doGraph2String(format: RDFFormat, statistics: String) =
      Try {
        logger.debug( s""">>>> doGraph2String format: '$format'""")
        logger.trace(">>>> triples: " + triples)
        graphWriter.writeGraph(triples.get, outputStream,
          format, prefix2uriMap.asJava)
        statistics + outputStream.toString()
      }
    def writeGeoJSON(triples: Try[Rdf#Graph]) = {
      val gr: org.apache.jena.graph.Graph = triples.get.asInstanceOf[org.apache.jena.graph.Graph]
      Success(rdfModelToGeoJSONstring(
        ModelFactory.createModelForGraph(gr)))
    }
    if (formatFromURL === "" )
    if (format != "ical" && format != "text/calendar") {
      val graphSize = triples.getOrElse(emptyGraph).size
      if (format === "jsonld")
        writeTryGraphBanana(triples, jsonldCompactedWriter, "")
      else if (format === "rdfxml") {
        val statistics = s"<!-- graph size ${graphSize} -->\n"
        doGraph2String(RDFFormat.RDFXML_PRETTY, statistics)
      } else if (format === ntMime ||
        format === "ntriples" ||
        format === "n-triples")
        writeTryGraphBanana(triples, ntriplesWriter, "")
      else if (format === geoJsonMIME ||
        format === "geojson") {
        writeGeoJSON(triples)
      } else
        doGraph2String(RDFFormat.TURTLE_PRETTY, s"# graph size ${graphSize}\n")
    } else
      Try(graph2iCalendar(triples.get))
    else {
      if( formatFromURL === geoJsonMIME)
        writeGeoJSON(triples)
      else if(formatFromURL === icalMime )
        Try(graph2iCalendar(triples.get))
      else {
      val jenaLang = RDFLanguages.contentTypeToLang(formatFromURL)
      logger.debug(s"formatFromURL $formatFromURL, jenaLang $jenaLang ; defaultSerialization ${RDFWriterRegistry.defaultSerialization(jenaLang)}")
      doGraph2String(
          RDFWriterRegistry.defaultSerialization(jenaLang), "")
      }
    }
  }

  private def writeTryGraphBanana(
    triples: Try[Rdf#Graph],
    writer:  RDFWriter[Rdf, Try, Object], stats: String): Try[String] = {
    triples.map {
      graph =>
        val tryString = writer.asString(graph, base = None ) // baseURI)
        //        logger.debug( s">>>> writeTryGraphBanana tryString $tryString" )
        tryString match {
          case Success(s) => stats + s
          case Failure(f) => s"writeTryGraphBanana: trouble in writing graph: $f"
        }
    }
  }

  /**
   * UNUSED, BANANA RDF version
   * RDF graph to String
   *  @param format "turtle" or "rdfxml" or "jsonld" or ntMime = "application/n-triples"
   *  TODO REFACTOR, use conneg helper RDFContentNegociation:
   *  then pass MIME type
   */
  private def graph2StringBANANA(triples: Try[Rdf#Graph], baseURI: String, format: String = "turtle"): Try[String] = {
    logger.info(s"graph2StringBANANA: base URI <$baseURI>, format '$format', triples ${triples.isSuccess}")
    triples match {
      case Success(graph) =>
        val graphSize = graph.size
        if (format != "ical") {
        val (writer, stats) =
          if (format === "jsonld")
            (jsonldCompactedWriter, "")
          else if (format === "rdfxml")
            (rdfXMLWriter, s"<!-- graph size ${graphSize} -->\n")
            else if (format === ntMime ||
                format === "ntriples" ||
                format === "n-triples" )
            (ntriplesWriter, "")
          else
            (turtleWriter, s"# graph size ${graphSize}\n")

//        logger.debug( s">>>> graph2StringBANANA writer $writer, stats $stats, baseURI $baseURI, graph $graph" )

        Success( stats + {
          val tryString = writer.asString(graph, base = None ) // baseURI)
//        logger.debug( s">>>> graph2StringBANANA tryString $tryString" )
          tryString match {
            case Success(s) => s
            case Failure(f) => s"graph2StringBANANA: trouble in writing graph: $f"
          }
        }
        )
        } else
          Success( graph2iCalendar(graph) )

      case Failure(f) => Failure(f)
    }
  }

  private lazy val owl = OWLPrefix[Rdf]
  private lazy val propTypes = List(rdf.Property, owl.ObjectProperty, owl.DatatypeProperty)

  def isProperty(uriTokeep: Rdf#Node): Boolean = {
    val types = quadQuery(uriTokeep, rdf.typ, ANY).toList
    logger.debug( s"isProperty( $uriTokeep ) : types $types" )
    types.exists { typ => propTypes.contains(typ._1.objectt) }
  }


  private lazy val classTypes = List(rdfs.Class, owl.Class)

  /** needs transaction ! */
  def isClass(uri: Rdf#Node): Boolean = {
    val typeQuads = quadQuery(uri, rdf.typ, ANY).toList
    logger.debug( s"isProperty( $uri ) : types $typeQuads" )
    val types = typeQuads.map { typ => typ._1.objectt }
    containsClassType(types)
  }

    def isClassTR(uri: Rdf#Node): Boolean = {
      wrapInReadTransaction{
        isClass(uri)
      } getOrElse(false)
    }

  def containsClassType( types: List[Rdf#Node]): Boolean =
    types.exists { typ => classTypes.contains(typ) }


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


