package deductions.runtime.jena

import org.w3.banana.jena.JenaModule
import org.w3.banana.jena.Jena
import org.w3.banana.RDF
import org.w3.banana.RDFOpsModule

import deductions.runtime.services.TypeAddition

import scala.collection.mutable.ArraySeq
import scala.util.Try
import scala.language.postfixOps

/**
 * deductions.runtime.jenaTypeAdditionApp
 * See trait TypeAddition;
 * types inferred from ontologies are added to objects of given subjects;
 * if no argument, do this on all objects.
 *
 * @author jmv
 */
object TypeAdditionApp extends JenaModule
    with App
    with RDFStoreLocalJena1Provider
    with TypeAdditionAppTrait[Jena, ImplementationSettings.DATASET] {
  val uris: ArraySeq[Rdf#URI] = args map { p => ops.URI(p) }
  run()
}

trait TypeAdditionAppTrait[Rdf <: RDF, DATASET]
    extends TypeAddition[Rdf, DATASET] {

  import ops._
  import sparqlOps._
  import rdfStore.transactorSyntax._
  import rdfStore.sparqlEngineSyntax._

  val uris: ArraySeq[Rdf#URI]

  /** non TRANSACTIONAL */
  def run() {
    println(s"""Types added for URI's $uris""")
    Try {
      if (uris isEmpty) {
        val tr = getTriples(allNamedGraph)
        add_types(tr.toIterator)
      } else {
        uris map { uri =>
          {
            println(s"""Types added for $uri""")
            val triples = find(allNamedGraph, uri, ANY, ANY)
            add_types(triples)
          }
        }
      }
    }
  }

  private def add_types(tr: Iterator[Rdf#Triple]) {
    println(s"""Types added for
    ${addTypes(tr.toSeq, None)}""")
  }
}
