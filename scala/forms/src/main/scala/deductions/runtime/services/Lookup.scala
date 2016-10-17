package deductions.runtime.services

import scala.util.Try
import org.w3.banana.RDF
import deductions.runtime.abstract_syntax.PreferredLanguageLiteral
import deductions.runtime.dataset.RDFStoreLocalProvider
import deductions.runtime.abstract_syntax.InstanceLabelsInferenceMemory
import deductions.runtime.utils.RDFPrefixes

/**
 * API for a lookup web service similar to dbPedia lookup
 */
trait Lookup[Rdf <: RDF, DATASET]
    extends RDFStoreLocalProvider[Rdf, DATASET]
    with InstanceLabelsInferenceMemory[Rdf, DATASET]
    with PreferredLanguageLiteral[Rdf]
    with SPARQLHelpers[Rdf, DATASET]
    with RDFPrefixes[Rdf] {
  // RDFHelpers[Rdf

  import ops._
  import sparqlOps._
  import rdfStore.sparqlEngineSyntax._
  import rdfStore.transactorSyntax._

  /**
   * This is dbPedia's output format, that could be used:
   *
   * <ArrayOfResult xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
   * xmlns:xsd="http://www.w3.org/2001/XMLSchema"
   * xmlns="http://lookup.dbpedia.org/">
   *  <Result>
   *    <Label>Jimi Hendrix</Label>
   *    <URI>http://dbpedia.org/resource/Jimi_Hendrix</URI>
   *    <Description> This article is about the guitarist. For the band, see The Jimi Hendrix Experience.</Description>
   *   <Classes>
   *    <Class>
   *     <Label>Person</Label>
   *     <URI>http://xmlns.com/foaf/0.1/Person</URI>
   *    </Class>
   */
  def lookup(search: String, lang: String = "en"): String = {
    val queryString = indexBasedQuery.makeQueryString(search)
    
    val res: List[Seq[Rdf#Node]] = sparqlSelectQueryVariables(queryString: String, Seq("thing") )
    println(s"lookup(search=$search $queryString => $res")
    println(s"lookup: starting TRANSACTION for dataset $dataset")
    val transaction = dataset.r({
      val urilangs = res.map {
        uris =>
          val uri = uris.head
//          val label = instanceLabelFromTDB(uri, lang)
        val label = instanceLabel(uri, allNamedGraph, lang)
        (uri, label)
      }
      urilangs
    })
    val elems = transaction.get.map {
      case (uri, label) =>
        <Result>
          <Label>{ label }</Label>
          <URI>{ uri }</URI>
        </Result>
    }
    val xml =
      <ArrayOfResult>
    	{ elems }
    </ArrayOfResult>
    xml.toString
    // lookupJSON(search)
  }

  /** use Lucene
   *  see https://jena.apache.org/documentation/query/text-query.html 
   *  TODO output rdf:type also
   *  */
  private val indexBasedQuery = new SPARQLQueryMaker[Rdf] {
    //          val rdfs = RDFSPrefix[Rdf]
    override def makeQueryString(search: String): String = s"""
         |PREFIX text: <http://jena.apache.org/text#>
         |${declarePrefix(rdfs)}
         |SELECT DISTINCT ?thing WHERE {
         |  graph ?g {
         |    ?thing text:query ( '${search.trim()}' 10 )
         |  }
         |}""".stripMargin
  }

  /**
   * OLD IMPLEMENTATION
   * Get simple JSON from a simple string search ( for completion in UI )
   *
   * Tested with
   * http://localhost:9000/lookup?q=Jean-Marc
   */
  private def lookupJSON(search: String): String = {
    val tryListString = dataset.r({
      implicit val listOfLists = search_string(search)
      val subjects = listOfLists.map { l => l.head }
      for (subject <- subjects) yield {
        val label = instanceLabelFromTDB(subject, "")
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
    sparqlSelectQueryVariablesNT(queryString, Seq("thing", "class"))
  }

}