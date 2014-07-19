package deductions.runtime.abstract_syntax
import org.w3.banana.RDF

trait FormFromInstance[Rdf <: RDF] {
  type Entry = FormModule[Rdf#URI]#Entry
  /** find fields from given Instance subject */
  def fields(subject: Rdf#URI, graph: Rdf#Graph): Seq[Entry] = {
    Seq[Entry]() // TODO
  }

}