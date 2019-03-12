package deductions.runtime.services

import deductions.runtime.utils.RDFStoreLocalProvider
import deductions.runtime.jena.{ImplementationSettings, RDFStoreLocalJenaProvider}
import deductions.runtime.jena.lucene.LuceneIndex
import deductions.runtime.utils.{DefaultConfiguration, FileUtils}
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import org.w3.banana.RDF
import org.w3.banana.io.{RDFReader, RDFXML}
import deductions.runtime.sparql_cache.SPARQLHelpers

import scala.language.postfixOps
import scala.util.Try

/** experiment with bindings in SPARQL executions */
trait TestSPARQLTrait[Rdf <: RDF, DATASET] extends FunSuite
    with BeforeAndAfterAll
    with RDFStoreLocalProvider[Rdf, DATASET]
    with SPARQLHelpers[Rdf, DATASET] {

  val rdfXMLReader: RDFReader[Rdf, Try, RDFXML]
  import ops._
  import rdfStore.sparqlEngineSyntax._
  import rdfStore.transactorSyntax._
  import sparqlOps._

  /**
   * populate TDB with a FOAF profile
   *  PASTED from TestStringSearchTrait
   */
  override def beforeAll {
    import ops._
    rdfStore.rw(dataset, {
      val base = "http://jmvanel.free.fr/jmv.rdf"
      val uri = base + "#me"
      val from = new java.net.URL(base).openStream()
      val graph = rdfXMLReader.read(from, base).get
      rdfStore.appendToGraph(dataset, makeUri(uri), graph)
    })
  }

  override def afterAll {
    FileUtils.deleteLocalSPARQL()
  }

  test("sparql 1") {

    val queryString = s"""
      ${declarePrefix(foaf)}
      SELECT *
      WHERE { GRAPH ?G {
#        ?SUBJECT foaf:firstName ?N .
        ?SUBJECT ?P ?O .
      }}
      """
    //    val bindings: Map[String, Rdf#Node] = Map("?SUBJECT" -> URI("http://jmvanel.free.fr/jmv.rdf#me"))
    val bindings: Map[String, Rdf#Node] = Map("?P" -> foaf.firstName)

    val transaction = dataset.r({
      // pasted from sparqlSelectQuery
      val solutionsTry = for {
        query <- (parseSelect(queryString))
        solutions <- (dataset.executeSelect(query, bindings))
      } yield solutions

      val result = solutionsTry.map {
        solutions =>
          val solsIterable = solutions.iterator.toIterable
          val r = solsIterable.headOption.map {
            row =>
              val names = row.varnames().toList
              val headerRow = names.map {
                name => Literal(name)
              }
              headerRow
          }
          val headerRow = r.toList

          val results = solsIterable map {
            row =>
              val variables = row.varnames().toList
              for (variable <- variables) yield row(variable) // .get.as[Rdf#Node].get
          }
          println("sparqlSelectQuery: after results")

          headerRow ++ results.to[List]
      }

      println(result)
    })
  }
}

//@Ignore
class TestSPARQL extends FunSuite
    with RDFStoreLocalJenaProvider
    with TestSPARQLTrait[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with DefaultConfiguration
    with LuceneIndex {
  val config = new DefaultConfiguration {}
}