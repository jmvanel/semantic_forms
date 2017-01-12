package deductions.runtime.abstract_syntax

import scala.util.Failure
import scala.util.Success

import org.w3.banana.RDF

import deductions.runtime.services.SPARQLHelpers

trait FormSyntaxFromSPARQL[Rdf <: RDF, DATASET]
    extends SPARQLHelpers[Rdf, DATASET]
    with FormModule[Rdf#Node, Rdf#URI]
    with FormSyntaxJson[Rdf] {

  self: FormSyntaxFactory[Rdf, DATASET] =>

  import ops._

  def createJSONFormFromSPARQL(query: String,
                               editable: Boolean = false,
                               formuri: String = ""): String = {
    formSyntax2JSON(createFormFromSPARQL(query, editable, formuri))
  }

  def createFormFromSPARQL(query: String,
                           editable: Boolean = false,
                           formuri: String = ""): FormSyntax = {

    val tryGraph = sparqlConstructQueryGraph(query)
    tryGraph match {
      case Success(graph) =>
        val triples = getTriples(graph).toSeq
        val fs = createFormFromTriples(triples, editable, formuri)(allNamedGraph)
        FormSyntax(subject = nullURI, fields = fs.fields, title = s"""From query:
      $query""")
      case Failure(f) => FormSyntax(subject = nullURI,
        fields = Seq(), title = s"""
            Failure: $f
            from query:
      $query""")
    }

  }

  private def createFormFromTriples(
    triples: Seq[Rdf#Triple],
    editable: Boolean = false,
    formuri: String = "")(implicit graph: Rdf#Graph): FormSyntax = {

    val formEntries = wrapInReadTransaction(
      triples.map {
        triple =>
          val (subject, prop, objet) = fromTriple(triple)
          makeEntryFromTriple(subject, prop, objet, formMode = FormMode(editable))
      }).get
    FormSyntax(subject = nullURI, fields = formEntries)
  }
}