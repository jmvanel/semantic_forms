package deductions.runtime.services

import java.io.ByteArrayOutputStream
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.Try
import org.apache.log4j.Logger
import org.w3.banana.RDF
import org.w3.banana.RDFOps
import org.w3.banana.RDFStore
import org.w3.banana.SparqlEngine
import org.w3.banana.SparqlOps
import org.w3.banana.TryW
import org.w3.banana.io.RDFWriter
import org.w3.banana.io.Turtle
import deductions.runtime.jena.RDFStoreObject
import deductions.runtime.dataset.RDFStoreLocalProvider
import org.w3.banana.TurtleWriterModule
import deductions.runtime.abstract_syntax.PreferredLanguageLiteral
import org.w3.banana.syntax._
import org.w3.banana.RDFOpsModule
import deductions.runtime.abstract_syntax.InstanceLabelsInference2

/**
 * API for a lookup web service similar to dbPedia lookup
 */
trait Lookup[Rdf <: RDF, DATASET]
    extends RDFStoreLocalProvider[Rdf, DATASET]
    with InstanceLabelsInference2[Rdf]
    with PreferredLanguageLiteral[Rdf] //extends RDFStoreLocalProvider[Rdf, DATASET]
    //    //    with PreferredLanguageLiteral[Rdf]
    //    with InstanceLabelsInference2[Rdf]
    //    with TurtleWriterModule 
    {
  //  implicit val ops: RDFOps[Rdf]

  import ops._
  import sparqlOps._
  import rdfStore.sparqlEngineSyntax._
  import rdfStore.transactorSyntax._

  /**
   * <ArrayOfResult xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns="http://lookup.dbpedia.org/">
   * <Result>
   * <Label>Jimi Hendrix</Label>
   * <URI>http://dbpedia.org/resource/Jimi_Hendrix</URI>
   * <Description> This article is about the guitarist. For the band, see The Jimi Hendrix Experience.</Description>
   * <Classes>
   * <Class>
   * <Label>http://xmlns.com/foaf/0.1/ person</Label>
   * <URI>http://xmlns.com/foaf/0.1/Person</URI>
   * </Class>
   */
  def lookup(search: String): String = {
    implicit val graph = search_only(search).get

    val triples = ops.getTriples(graph.asInstanceOf[Rdf#Graph])
    val subjects = triples.map { _.subject }
    val r1 = for (subject <- subjects) yield {
      val label = instanceLabel(subject, graph, "")
      s"""
        label: "$label""
        uri: "${subject}"
        description: ""
      """
    }
    s"""{ result: [
       ${r1}
    ]}"""
  }

  /**
   * NON transactional
   */
  private def search_only(search: String): Try[Rdf#Graph] = {
    val queryString = s"""
         |CONSTRUCT { ?thing ?p ?o; a ?CLASS } WHERE {
         |  {
         |  graph ?g {
         |    ?thing ?p ?o .
         |    FILTER regex( ?o, "$search", 'i')
         |  }
         |  } UNION {
         |  graph ?g0 {
         |    ?thing a ?CLASS .
         |  }
         |  }
         |}""".stripMargin
    println("search_only " + queryString)
    sparqlConstructQuery(queryString)
  }

  /**
   * NON transactional
   *  TODO copied
   */
  private def sparqlConstructQuery(queryString: String): Try[Rdf#Graph] = {
    val r = for {
      query <- parseConstruct(queryString)
      es <- dataset.executeConstruct(query, Map())
    } yield es
    r
  }

}