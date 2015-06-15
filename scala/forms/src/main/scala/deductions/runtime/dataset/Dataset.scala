package deductions.runtime.dataset

/**
 * @author jmv
 */

/**
 * A typeclass for [RDF Datasets](http://www.w3.org/TR/rdf11-concepts/#h2_section-dataset)
 *
 *  NOTE: Dataset could become a type within RDF, which would be used instead of A in
 *  trait GraphStore[Rdf <: RDF, M[+_], A]
 */
trait Dataset