package deductions.runtime.abstract_syntax

import org.w3.banana.RDF

/** populate Fields in form by inferring possible values from given rdfs:range's URI,
 *  through owl:oneOf and know instances */
trait RangeInference[Rdf <: RDF] {
  self: FormSyntaxFactory[Rdf] =>

  def addPossibleValues( entry:Entry, ranges: Set[Rdf#URI]) : Entry
     = entry // TODO modify entry to populate possibleValues
}