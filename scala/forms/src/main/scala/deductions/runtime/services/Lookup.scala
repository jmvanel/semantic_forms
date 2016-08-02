package deductions.runtime.services

import scala.util.Try
import org.w3.banana.RDF
import deductions.runtime.abstract_syntax.PreferredLanguageLiteral
import deductions.runtime.dataset.RDFStoreLocalProvider
import deductions.runtime.abstract_syntax.InstanceLabelsInferenceMemory

/**
 * API for a lookup web service similar to dbPedia lookup
 */
trait Lookup[Rdf <: RDF, DATASET]
    extends RDFStoreLocalProvider[Rdf, DATASET]
    with InstanceLabelsInferenceMemory[Rdf, DATASET]
    with PreferredLanguageLiteral[Rdf]
    with SPARQLHelpers[Rdf, DATASET] {

  import ops._
  import sparqlOps._
  import rdfStore.sparqlEngineSyntax._
  import rdfStore.transactorSyntax._

  /**
   * Get simple JSON from a simple string search ( for completion in UI )
   *
   * Tested with
   *  http://localhost:9000/lookup?q=Jean-Marc
   *
   * This is dbPedia's output format, that could be used:
   *
   * <ArrayOfResult xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
   * xmlns:xsd="http://www.w3.org/2001/XMLSchema"
   * xmlns="http://lookup.dbpedia.org/">
   *  <Result>
   *   <Label>Jimi Hendrix</Label>
   *    <URI>http://dbpedia.org/resource/Jimi_Hendrix</URI>
   *    <Description> This article is about the guitarist. For the band, see The Jimi Hendrix Experience.</Description>
   *   <Classes>
   *    <Class>
   *     <Label>http://xmlns.com/foaf/0.1/ person</Label>
   *     <URI>http://xmlns.com/foaf/0.1/Person</URI>
   *    </Class>
   */
  def lookup(search: String): String = {
    val tryListString = dataset.r({
      implicit val listOfLists = search_string(search)
      val subjects = listOfLists.map { l => l.head }
      for (subject <- subjects) yield {
        val label = instanceLabelFromTDB(subject, "")
        // TODO output rdf:type also
        s"""
        "label": "$label",
        "uri": "${subject}",
        "description": ""
      """
      }
    })
    s"""{ "result": [
       ${tryListString.get.mkString("{", "},\n", "}\n")}
    ]}"""
  }

  /**
   * NON transactional
   */
  private def search_string(search: String): List[Seq[Rdf#Node]] = {
    val queryString = s"""
         |select distinct ?thing ?class WHERE {
         |  {
         |  graph ?g {
         |    ?thing ?p ?o .
         |    FILTER regex( ?o, "$search", 'i')
         |  }
         |  } OPTIONAL {
         |  graph ?g0 {
         |    ?thing a ?class .
         |  }
         |  }
         |}""".stripMargin
//    println("search_only " + queryString)
    sparqlSelectQueryVariablesNT(queryString, Seq("thing", "class") )
  }

}