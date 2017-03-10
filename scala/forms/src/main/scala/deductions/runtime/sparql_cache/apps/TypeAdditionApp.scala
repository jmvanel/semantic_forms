package deductions.runtime.sparql_cache.apps

import scala.collection.mutable.ArraySeq
import scala.language.postfixOps
import scala.util.Try
import org.w3.banana.RDF

import deductions.runtime.services.TypeAddition
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.services.DefaultConfiguration

/**
 * deductions.runtime.jenaTypeAdditionApp
 * See trait TypeAddition;
 * types inferred from ontologies are added to objects of given subjects;
 * if no argument, do this on all objects.
 *
 * @author jmv
 */
object TypeAdditionApp extends  {
  override val config = new DefaultConfiguration {
    override val useTextQuery = false
  }
} with ImplementationSettings.RDFModule
    with App
    with ImplementationSettings.RDFCache
    with TypeAdditionAppTrait[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {


	import config._
  val uris: ArraySeq[Rdf#URI] = args map { p => ops.URI(p) }
  run()
}

trait TypeAdditionAppTrait[Rdf <: RDF, DATASET]
    extends TypeAddition[Rdf, DATASET] {

  import ops._

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
