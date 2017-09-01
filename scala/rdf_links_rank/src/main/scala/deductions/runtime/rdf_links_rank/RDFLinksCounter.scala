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
                       linksCountGraphURI: Rdf#URI = defaultLinksCountGraphURI) =
    Future {
      updateLinksCountNoFuture(databaseChanges, linksCountDataset, linksCountGraphURI)
    }

  /** update RDF Links Count, typically after user edits,
   *  transactional  */
  private def updateLinksCountNoFuture (
    databaseChanges: DatabaseChanges[Rdf],
    linksCountDataset: DATASET,
    linksCountGraphURI: Rdf#URI) = {

    val countsSubjectsToAddSet = mutable.Set[Rdf#Node]()
    val countsSubjectsToRemoveSet = mutable.Set[Rdf#Node]()
    val countsMap = mutable.Map[Rdf#Node, Int]()
    val countsToRemoveMap = mutable.Map[Rdf#Node, Int]()

    /* count Changes */
    def countChanges(triplesChanged: Seq[Rdf#Triple],
                     countsSubjectsSet: mutable.Set[Rdf#Node],
                     countsMap: mutable.Map[Rdf#Node, Int]) =
      for (
        linksCountGraph <- rdfStore.getGraph(linksCountDataset, linksCountGraphURI);
        tripleToAdd <- triplesChanged;
        subject = tripleToAdd.subject if (tripleToAdd.objectt.isURI)
      ) {
        countsSubjectsSet.add(subject)
        countsMap.put(subject, countsMap.getOrElse(subject, 0) + 1)
        println(s"countChanges: countsSubjectsSet ${countsSubjectsSet}, countsMap $countsMap")
      }

    rdfStore.r(linksCountDataset, {
      countChanges(databaseChanges.triplesToAdd, countsSubjectsToAddSet, countsMap)
      countChanges(databaseChanges.triplesToRemove, countsSubjectsToRemoveSet, countsToRemoveMap)
    })

    rdfStore.rw(linksCountDataset, {
      for (
        subject <- (countsSubjectsToAddSet ++ countsSubjectsToRemoveSet);
        linksCountGraph <- rdfStore.getGraph(linksCountDataset, linksCountGraphURI);
        oldCount <- getCountFromTDBTry(linksCountGraph, subject) ;
        _ = println(s"updateLinksCount: oldCount $oldCount")
      ) {
    	  println(s"countChanges 2: countsSubjectsToAddSet ${countsSubjectsToAddSet}, countsMap $countsMap")
        val count = oldCount + countsMap.getOrElse(subject, 0) -
                       countsToRemoveMap.getOrElse(subject, 0)
        println(s"updateLinksCount: count $count")
        if (count != oldCount) {
          rdfStore.removeTriples(linksCountDataset, linksCountGraphURI,
            Seq(Triple(subject,
              linksCountPred,
              IntToLiteral(ops).toLiteral(oldCount))))
          rdfStore.appendToGraph(linksCountDataset, linksCountGraphURI,
            makeGraph(Seq(Triple(
              subject,
              linksCountPred,
              IntToLiteral(ops).toLiteral(count)))))
        }
      }
    })
  }

  private def getCountFromTDBTry(linksCountGraph: Rdf#Graph,
                                 subject: Rdf#Node): Try[Int] = {
    val pg = PointedGraph(subject, linksCountGraph)
    println(s"getCountFromTDBTry: pg $pg")
    val pg1 = (pg / linksCountPred)
    println(s"getCountFromTDBTry: (pg / linksCountPred) ${pg1}")
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

    val solutionsTry = for {
      query <- parseSelect(query)
      solutions <- rdfStore.executeSelect(dataset, query, immutable.Map())
    } yield solutions

    val subjectCountIterator = solutionsTry match {
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
        System.err.println("computeLinksCount: " + f)
        Seq((URI(""), 0)).toIterator
    }

    /* TODO would like to avoid both:
     * - creating the graph in memory :(
     * - calling appendToGraph for each triple
    */
    val tripleIterator = subjectCountIterator.map {
      case (s, count) =>
        Triple(
          s,
          linksCountPred,
          IntToLiteral(ops).toLiteral(count))
    }
    rdfStore.appendToGraph(linksCountDataset, linksCountGraphURI,
      makeGraph(tripleIterator.toIterable))
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
         |   }
         |""".stripMargin
    println( s"resetRDFLinksCounts: $query" )

    rdfStore.rw(linksCountDataset, {
      sparqlUpdateQuery(query, linksCountDataset)
    })
  }

  /** pasted from module sparql_cache, thus avoiding dependency */
  private def sparqlUpdateQuery(queryString: String, dataset: DATASET): Try[Unit] = {
    for {
      query <- parseUpdate(queryString);
      es <- rdfStore.executeUpdate(dataset, query, immutable.Map())
    } yield es
  }
}
