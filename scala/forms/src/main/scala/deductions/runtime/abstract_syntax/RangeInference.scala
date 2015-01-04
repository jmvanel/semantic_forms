package deductions.runtime.abstract_syntax

import org.w3.banana.RDF
import org.w3.banana.OWLPrefix
import deductions.runtime.utils.RDFHelpers

/**
 * populate Fields in form by inferring possible values from given rdfs:range's URI,
 *  through owl:oneOf and know instances
 */
trait RangeInference[Rdf <: RDF] {
  self: FormSyntaxFactory[Rdf] =>

  def addPossibleValues(entry: Entry, ranges: Set[Rdf#URI] //, formSyntax:FormSyntax[Rdf#Node, Rdf#URI]
  ): Entry = {
    val owl = OWLPrefix[Rdf]
    val gr = graph
    val rdfh = new RDFHelpers[Rdf] { val graph = gr }

    /* modify entry to populate possibleValues,
     * by taking ?LIST from triples:
     * ?RANGE owl:oneOf ?LIST */
    def populateFromOwlOneOf(entry: ResourceEntry): ResourceEntry = {
      val possibleValues = scala.collection.mutable.ArrayBuffer[Rdf#URI]()
      for (range <- ranges) {
        val enumerated = ops.getObjects(graph, range, owl.oneOf)
        for (enum <- enumerated)
          ops.foldNode(enum)(
            uri => {
              val list = rdfh.nodeSeqToURISeq(rdfh.rdfListToSeq(Some(uri)))
              possibleValues.appendAll(list)
            },
            x => (), x => ())

      }
      entry.copy(possibleValues = possibleValues)
    }

    // TODO modify entry to populate possibleValues
    def populateFromInstances(entry: ResourceEntry): Entry = {
      entry
    }

    entry match {
      case r: ResourceEntry =>
        populateFromInstances(
          populateFromOwlOneOf(r))
      case _ => entry
    }

  }
}