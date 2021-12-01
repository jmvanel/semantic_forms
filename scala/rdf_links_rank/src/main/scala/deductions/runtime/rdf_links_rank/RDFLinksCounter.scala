package deductions.runtime.rdf_links_rank

import scala.util.Try

import org.w3.banana.RDF
import org.w3.banana.RDFOps
import org.w3.banana.RDFStore
import org.w3.banana.syntax.RDFSyntax
import org.w3.banana.binder.FromLiteral
import org.w3.banana.PointedGraph
import org.w3.banana.binder.ToLiteral
import org.w3.banana.binder.FromLiteral
import org.w3.banana.SparqlOps

import collection._
import scala.util.Success
import scala.util.Failure

import java.rmi.UnexpectedException

import deductions.runtime.utils.RDFPrefixes
import deductions.runtime.utils.DatabaseChanges
import scala.concurrent.Future
import org.w3.banana.SparqlUpdate

import scalaz._
import Scalaz._

trait RDFLinksCounter[Rdf <: RDF, DATASET]
    extends RDFPrefixes[Rdf]
    with RDFSyntax[Rdf] {
//  self: RDFSyntax[Rdf] =>

  val linksCountPred = form("linksCount")
  val defaultLinksCountGraphURI = ops.URI("linksCountGraph:")

  implicit val ops: RDFOps[Rdf]
  implicit val rdfStore: RDFStore[Rdf, Try, DATASET] with SparqlUpdate[Rdf, Try, DATASET]

  import ops._
  implicit val sparqlOps: SparqlOps[Rdf]
  import sparqlOps._
  import ToLiteral._
  import FromLiteral._

  import scala.concurrent.ExecutionContext.Implicits.global

  /** update RDF Links Count, typically after user edits,
   *  in a Future; transactional */
  def updateLinksCount(databaseChanges: DatabaseChanges[Rdf],
                       linksCountDataset: DATASET,
                       linksCountGraphURI: Rdf#URI = defaultLinksCountGraphURI,
                       replaceCount: Boolean = false) =
    Future {
      updateLinksCountNoFuture(databaseChanges, linksCountDataset, linksCountGraphURI, replaceCount)
    }

  /** update RDF Links Count, typically after user edits,
   *  transactional  */
  private def updateLinksCountNoFuture (
    databaseChanges: DatabaseChanges[Rdf],
    linksCountDataset: DATASET,
    linksCountGraphURI: Rdf#URI,
    replaceCount: Boolean) = {

    val countURIsToAddSet = mutable.Set[Rdf#Node]()
    val countURIsToRemoveSet = mutable.Set[Rdf#Node]()
    val countsMap = mutable.Map[Rdf#Node, Int]()
    val countsToRemoveMap = mutable.Map[Rdf#Node, Int]()

    val countReverseURIsToAddSet = mutable.Set[Rdf#Node]()

    val indexSubject: Rdf#Triple => Option[Rdf#Node] =
      tr => if (tr.objectt.isURI) Some(tr.subject) else None
    val indexObject: Rdf#Triple => Option[Rdf#Node] =
      tr => if (tr.objectt.isURI) Some(tr.objectt) else None

    /* count Changes in given triples, and put results in given Set and Map */
    def countChanges(
      triplesChanged: Seq[Rdf#Triple],
      countsURISet: mutable.Set[Rdf#Node],
      countsMap: mutable.Map[Rdf#Node, Int],
      uriToIndex: Rdf#Triple => Option[Rdf#Node] = indexSubject) =
      for (
        linksCountGraph <- rdfStore.getGraph(linksCountDataset, linksCountGraphURI);
        tripleToAdd <- triplesChanged;
        uri <- uriToIndex(tripleToAdd)
      ) {
        countsURISet.add(uri)
        val newCount = countsMap.getOrElse(uri, 0) + 1
        countsMap.put(uri, newCount)
        logger.debug(s"countChanges: URI ${uri} -> count $newCount")
      }

    rdfStore.r(linksCountDataset, {
      countChanges(databaseChanges.triplesToAdd, countURIsToAddSet, countsMap)
      countChanges(databaseChanges.triplesToRemove, countURIsToRemoveSet, countsToRemoveMap)

      countChanges(databaseChanges.triplesToAdd, countReverseURIsToAddSet, countsMap,
          indexObject)
      countChanges(databaseChanges.triplesToRemove, countURIsToRemoveSet, countsToRemoveMap,
          indexObject)
    })

    logger.debug(s"""updateLinksCount: countURIsToAddSet ${countURIsToAddSet.mkString(", ")},
      countsMap $${countsMap.mkString(", ")}""")

    def replaceCountsInTDB(uris: Set[Rdf#Node], replaceCount: Boolean=replaceCount) =
    rdfStore.rw(linksCountDataset, {
      for (
        uri <- uris;
        linksCountGraph <- rdfStore.getGraph(linksCountDataset, linksCountGraphURI);
        oldCount <- getCountFromTDBTry(linksCountGraph, uri)
        ; _ = logger.debug(s"updateLinksCount: oldCount $oldCount")
      ) {

        val increment = countsMap.getOrElse(uri, 0) -
                        countsToRemoveMap.getOrElse(uri, 0)
        val count =
          if(replaceCount)
        	  increment
        	else
        	 oldCount + increment
        logger.debug(s"updateLinksCount: URI $uri , oldCount $oldCount, count $count")

        if (count  =/=  oldCount) {
          rdfStore.removeTriples(linksCountDataset, linksCountGraphURI,
            Seq(Triple(uri,
              linksCountPred,
              IntToLiteral(ops).toLiteral(oldCount))))
          rdfStore.appendToGraph(linksCountDataset, linksCountGraphURI,
            makeGraph(
            		Seq(Triple(uri,
              linksCountPred,
              IntToLiteral(ops).toLiteral(count)))))
        }
      }
    })

    replaceCountsInTDB(countURIsToAddSet ++ countURIsToRemoveSet)
    replaceCountsInTDB(countReverseURIsToAddSet, replaceCount=false)
  }

  private def getCountFromTDBTry(linksCountGraph: Rdf#Graph,
                                 subject: Rdf#Node): Try[Int] = {
    val pg = PointedGraph(subject, linksCountGraph)
    logger.debug(s"replaceCountsInTDB: getCountFromTDBTry: pg $pg")
    val pg1 = (pg / linksCountPred)
    logger.debug(s"replaceCountsInTDB: getCountFromTDBTry: (pg / linksCountPred) ${pg1}")
    if (pg1.nodes.isEmpty)
      Try(0)
    else
      pg1.as[Int]
  }

  /** compute RDF Links Count from scratch, typically called in batch */
  def computeLinksCount(
    dataset: DATASET,
    linksCountDataset: DATASET,
    linksCountGraphURI: Rdf#URI = defaultLinksCountGraphURI) = {

    val query = """
      |SELECT DISTINCT ?S ( COUNT(?O) + COUNT(?S1) AS ?COUNT)
      |WHERE {
      |  { GRAPH ?GR {
      |    ?S ?P ?O .
      |        FILTER ( isURI(?O) )
      | } } UNION
      |  { GRAPH ?GR1 {
      |      ?S1 ?P1 ?S .
      |  } }
      |}
      |GROUP BY ?S
      |ORDER BY DESC(?COUNT)""".stripMargin

    val subjectCountIterator =
      rdfStore.r(dataset, {

        val solutionsTry = for {
          query <- parseSelect(query)
          solutions <- rdfStore.executeSelect(dataset, query, immutable.Map())
        } yield solutions

        solutionsTry match {
          case Success(solutions) =>
            val counts = for (
              solution <- solutions.toIterable;
              //          v = solution.varnames ;
              nodeIntPairTry = for (
                // cf SparqlSolutionSyntaxW
                s <- solution("?S");
                countNode <- solution("?COUNT");
                count <- foldNode(countNode)(
                  _ => Failure(new UnexpectedException("computeLinksCount")),
                  _ => Failure(new UnexpectedException("computeLinksCount")),
                  literal => IntFromLiteral.fromLiteral(literal))
              ) yield { (s, count) } if (nodeIntPairTry.isSuccess)
            ) yield { nodeIntPairTry.toOption.get }
            counts
          case Failure(f) =>
            logger.error("computeLinksCount: " + f)
            Seq((URI(""), 0)).iterator
        }
      })

    logger.debug(s"computeLinksCount: After executeSelect")

    if (subjectCountIterator.isFailure)
      logger.error(s"computeLinksCount: subjectCountIterator: $subjectCountIterator")

    /* TODO would like to avoid both:
     * - creating the graph in memory :(
     * - calling appendToGraph for each triple
    */
    val tripleIterator = subjectCountIterator.get.map {
      case (s, count) =>
        Triple(
          s,
          linksCountPred,
          IntToLiteral(ops).toLiteral(count))
    }
    val tripleIterable = tripleIterator.iterator.to(Iterable)
    rdfStore.rw(dataset, {
      rdfStore.appendToGraph(linksCountDataset, linksCountGraphURI,
        makeGraph(tripleIterable))
    })
    logger.debug(s"computeLinksCount: ${tripleIterable.size} counts added in graph $linksCountGraphURI .")
  }

  def resetRDFLinksCounts(
    dataset: DATASET,
    linksCountDataset: DATASET,
    linksCountGraphURI: Rdf#URI = defaultLinksCountGraphURI) = {
    val query = s"""
         | DELETE {
         |   graph ?GR {
         |     ?S <${fromUri(linksCountPred)}> ?O .
         |   }}
         | WHERE {
         |   graph ?GR {
         |     ?S <${fromUri(linksCountPred)}> ?O .
         |   }}
         |""".stripMargin
    logger.debug(s"resetRDFLinksCounts: $query")
    logger.debug(s"resetRDFLinksCounts: result: ${
      rdfStore.rw(linksCountDataset, {
        sparqlUpdateQuery(query, linksCountDataset)
      })
    }")
  }

  /** pasted from module sparql_cache, thus avoiding dependency */
  private def sparqlUpdateQuery(queryString: String, dataset: DATASET): Try[Unit] = {
    for {
      query <- parseUpdate(queryString);
      es <- rdfStore.executeUpdate(dataset, query, immutable.Map())
    } yield es
  }
}
