package deductions.runtime.jena

import org.w3.banana.jena.JenaModule
import deductions.runtime.services.TypeAddition
import org.w3.banana.jena.Jena
import com.hp.hpl.jena.query.Dataset

/**
 * deductions.runtime.jenaTypeAdditionApp
 * See trait TypeAddition;
 * types inferred from ontologies are added to objects of given subjects;
 * if no argument, do this on all objects.
 *
 * @author jmv
 */
object TypeAdditionApp extends App with JenaModule with RDFStoreLocalJena1Provider
    with TypeAddition[Jena, Dataset] {
  import ops._
  val uris = args map { p => URI(p) }

  if (uris isEmpty) {
//    val tr = find(allNamedGraph, ANY, ANY, ANY)
    val tr = ops.getTriples(allNamedGraph)
    add_types(tr.toIterator)
  } else {
    uris map { uri =>
      {
        val tr = find(allNamedGraph, uri, ANY, ANY)
        add_types(tr)
      }
    }
  }

  private def add_types(tr: Iterator[Rdf#Triple]) {
    println(s"""Types added for
    ${addTypes(tr.toSeq, None)}""")
  }
}