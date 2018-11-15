package deductions.runtime.abstract_syntax

import deductions.runtime.core.FormModule
import deductions.runtime.sparql_cache.SPARQLHelpers
import deductions.runtime.core.HTTPrequest

import org.w3.banana.RDF

import scala.util.{Failure, Success}

trait FormSyntaxFromSPARQL[Rdf <: RDF, DATASET]
    extends SPARQLHelpers[Rdf, DATASET]
    with FormModule[Rdf#Node, Rdf#URI]
    with FormSyntaxJson[Rdf] {

  self: FormSyntaxFactory[Rdf, DATASET] =>

  import ops._

  /** create JSON Form From SPARQL */
  def createJSONFormFromSPARQL(
    query: String,
    editable: Boolean = false,
    formuri: String = "",
    request: HTTPrequest): String = {
    formSyntax2JSONString(
      createFormFromSPARQL (
        query, editable, formuri, request))
  }

  /** create FormSyntax from result of CONSTRUCT SPARQL query */
  def createFormFromSPARQL(query: String,
                           editable: Boolean = false,
                           formuri: String = "",
                           request: HTTPrequest): FormSyntax = {
    logger.debug( s"""query:
        $query""")
    val tryGraph = sparqlConstructQueryGraph(
          query,
          context=request.queryString2 )
    logger.debug( s"tryGraph: $tryGraph")
    tryGraph match {
      case Success(graph) =>
        val triples = getTriples(graph).toSeq
        val fs = wrapInTransaction {
          createFormFromTriples(triples, editable, formuri)(allNamedGraph,
              request.getLanguage)
        } . get
        FormSyntax(subject = nullURI, fields = fs.fields, title = s"""From query:
      $query""")
      case Failure(f) => FormSyntax(subject = nullURI,
        fields = Seq(), title = s"""
            Failure: $f
            from query:
      $query""")
    }

  }

  /** create FormSyntax from triples; needs READ transaction */
  def createFormFromTriples(
    triples: Seq[Rdf#Triple],
    editable: Boolean = false,
    formuri: String = "")(implicit graph: Rdf#Graph, lang:String): FormSyntax = {
    logger.debug( s"createFormFromTriples: triples: $triples")
    logger.info( s"createFormFromTriples: triples count: ${triples.size}")
    val formEntries =
      triples.map {
        triple =>
          val (subject, prop, objet) = fromTriple(triple)
          makeEntryFromTriple(subject, prop, objet, formMode = FormMode(editable))
      }
    FormSyntax(subject = nullURI, fields = formEntries)
  }
}