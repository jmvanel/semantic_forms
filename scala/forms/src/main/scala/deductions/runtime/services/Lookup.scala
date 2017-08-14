package deductions.runtime.services

import deductions.runtime.abstract_syntax.{InstanceLabelsInferenceMemory, PreferredLanguageLiteral}
import deductions.runtime.sparql_cache.SPARQLHelpers
import deductions.runtime.sparql_cache.dataset.RDFStoreLocalProvider
import deductions.runtime.utils.RDFPrefixes
import org.w3.banana.RDF
import play.api.libs.json.Json

/**
 * API for a lookup web service similar to dbPedia lookup
 * 
 * TODO common code with StringSearchSPARQL
 */
trait Lookup[Rdf <: RDF, DATASET]
    extends RDFStoreLocalProvider[Rdf, DATASET]
    with InstanceLabelsInferenceMemory[Rdf, DATASET]
    with PreferredLanguageLiteral[Rdf]
    with SPARQLHelpers[Rdf, DATASET]
    with RDFPrefixes[Rdf] {

  type Results = List[(Rdf#Node, String, String, String, String)]

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
    
    val res = searchStringOrClass(search, clas)

    println(s"lookup(search=$search, clas=<$clas> => $res")
    println(s"lookup: after searchStringOrClass, starting TRANSACTION for dataset $dataset")
    val transaction = rdfStore.rw( dataset, {
      val urilabels = res.map {
        uris =>
          val uri = uris.head
          val label = makeInstanceLabel(uri, allNamedGraph, lang)
          val desc = instanceDescription(uri, allNamedGraph, lang)
          val img = instanceImage(uri, allNamedGraph, lang)
          val typ = instanceTypeLabel(uri, allNamedGraph, lang)
          (uri, label, desc, img, typ)
      }
      urilabels
    })
    println(s"lookup: leaved TRANSACTION for dataset $dataset")
    val list = transaction.get

    if (mime.contains("xml"))
      formatXML(list)
    else
      formatJSON(list)
  }

  private def formatXML(list: Results): String = {
    val elems = list.map {
      case (uri, label, desc, img, typ) =>
        <Result>
          <Label>{ label }</Label>
          <URI>{ uri }</URI>
          <Description>{ desc }</Description>
          <Image>{ img }</Image>
          <Type>{ typ }</Type>
        </Result>
    }
    val xml =
      <ArrayOfResult>
        { elems }
      </ArrayOfResult>
    xml.toString
  }

  /** The keys are NOT exactly the same as the XML tags :( */
  private def formatJSON(list: Results): String = {
    val list2 = list.map {
      case (uri, label, desc, img, typ) => Json.obj(
          "label" -> label,
          "uri" -> uri.toString(),
          "description" -> desc,
          "image" -> img,
          "type" -> typ
          )
    }
    println(s"list2 $list - $list2")
    val results =  Json.obj( "results" -> list2 )
    Json.prettyPrint(results)
  }

  /**
   * use Lucene
   *  see https://jena.apache.org/documentation/query/text-query.html
   *  TODO output rdf:type also
   */
  private val indexBasedQuery = new SPARQLQueryMaker[Rdf] {
    override def makeQueryString(searchStrings: String*): String = {
      val search = searchStrings(0)
      val clas = if( searchStrings.size > 1 ) {
        val classe = searchStrings(1)
        if( classe == "" ) "?CLASS"
        else "<" + expandOrUnchanged(classe) + ">"
      } else "?CLASS"

      // TODO pasted from StringSearchSPARQL :((((
      val textQuery =
        if (search.length() > 0) {
          val searchStringPrepared = prepareSearchString(search).trim()
          if (config.useTextQuery)
            s"?thing text:query ( '$searchStringPrepared' ) ."
          else
            s"""    ?thing ?P1 ?O1 .
              FILTER ( regex( str(?O1), '$searchStringPrepared' ) )"""
        } else ""

      val queryWithlinksCount = s"""
         |${declarePrefix(text)}
         |${declarePrefix(rdfs)}
         |SELECT DISTINCT ?thing (COUNT(*) as ?count) WHERE {
         |  graph ?g {
         |    $textQuery
         |    ?thing ?p ?o .
         |    ?thing a $clas .
         |  }
         |}
         |GROUP BY ?thing
         |ORDER BY DESC(?count)
         |LIMIT 10
         |""".stripMargin

      val queryWithoutlinksCount = s"""
         |${declarePrefix(text)}
         |${declarePrefix(rdfs)}
         |SELECT DISTINCT ?thing WHERE {
         |  graph ?g {
         |    $textQuery
         |    ?thing ?p ?o .
         |    ?thing a $clas .
         |  }
         |}
         |LIMIT 15
         |""".stripMargin

     return queryWithoutlinksCount
    }
  }


  import scala.language.reflectiveCalls

  /** search String Or Class
   * transactional
   */
  def searchStringOrClass(search: String, clas: String = ""): List[Seq[Rdf#Node]] = {
    val queryString = indexBasedQuery.makeQueryString(search, clas)
    println(s"""searchStringOrClass(search="$search", clas <$clas>, queryString "$queryString" """)
    val res: List[Seq[Rdf#Node]] = sparqlSelectQueryVariables(queryString, Seq("thing"))
    res
  }

}