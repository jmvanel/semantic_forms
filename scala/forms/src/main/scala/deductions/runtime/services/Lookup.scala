package deductions.runtime.services

import scala.util.Try
import org.w3.banana.RDF
import deductions.runtime.abstract_syntax.PreferredLanguageLiteral
import deductions.runtime.dataset.RDFStoreLocalProvider
import deductions.runtime.abstract_syntax.InstanceLabelsInferenceMemory
import deductions.runtime.utils.RDFPrefixes
import play.api.libs.json.Json
import play.api.libs.json.JsArray

/**
 * API for a lookup web service similar to dbPedia lookup
 */
trait Lookup[Rdf <: RDF, DATASET]
    extends RDFStoreLocalProvider[Rdf, DATASET]
    with InstanceLabelsInferenceMemory[Rdf, DATASET]
    with PreferredLanguageLiteral[Rdf]
    with SPARQLHelpers[Rdf, DATASET]
    with RDFPrefixes[Rdf] {

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
  def lookup(search: String, lang: String = "en", clas: String = "", mime: String): String = {
    
//    val queryString0 = indexBasedQuery.makeQueryString(search)
//    val queryString = if (clas != "") {
//      queryString0.replaceFirst("""\?class""", "<" + expandOrUnchanged(clas) + ">")
//    } else queryString0
//    println(s"lookup(search=$search, clas $clas, queryString $queryString")
//    val res: List[Seq[Rdf#Node]] = sparqlSelectQueryVariables(queryString, Seq("thing"))
    
    val res = searchStringOrClass(search, clas)

    println(s"lookup(search=$search, clas=$clas => $res")
    println(s"lookup: starting TRANSACTION for dataset $dataset")
    val transaction = rdfStore.r( dataset, {
      val urilangs = res.map {
        uris =>
          val uri = uris.head
          val label = instanceLabel(uri, allNamedGraph, lang)
          (uri, label)
      }
      urilangs
    })
    val list = transaction.get
    if (mime.contains("json"))
      formatJSON(list)
    else
      formatXML(list)
  }

  private def formatXML(list: List[(Rdf#Node, String)]): String = {
    val elems = list.map {
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
  }

  private def formatJSON(list: List[(Rdf#Node, String)]): String = {
    val list2 = list.map {
      case (uri, label) => Json.obj("Label" -> label, "URI" -> uri.toString())
    }
    val responses = new JsArray(list2)
    Json.prettyPrint(responses)
  }

  /**
   * use Lucene
   *  see https://jena.apache.org/documentation/query/text-query.html
   *  TODO output rdf:type also
   */
  private val indexBasedQuery = new SPARQLQueryMaker[Rdf] {
    override def makeQueryString(searchStrings: String*): String = {
      val search = searchStrings(0)
      val clas = if( searchStrings.size > 1 )
        "<" + expandOrUnchanged(searchStrings(1)) + ">"
        else "?CLASS"
      s"""
         |${declarePrefix(text)}
         |${declarePrefix(rdfs)}
         |SELECT DISTINCT ?thing (COUNT(*) as ?count) WHERE {
         |  graph ?g {
         |    ?thing text:query '${prepareSearchString(search).trim()}' .
         |    ?thing ?p ?o .
         |    ?thing a $clas .
         |  }
         |}
         |GROUP BY ?thing
         |ORDER BY DESC(?count)
         |LIMIT 10
         |""".stripMargin
    }
  }


  import scala.language.reflectiveCalls

  /** search String Or Class
   * transactional
   */
  def searchStringOrClass(search: String, clas: String = ""): List[Seq[Rdf#Node]] = {
    val queryString = indexBasedQuery.makeQueryString(search, clas)
    println(s"searchStringOrClass(search=$search, clas $clas, queryString $queryString")
    val res: List[Seq[Rdf#Node]] = sparqlSelectQueryVariables(queryString, Seq("thing"))
    res
  }

}