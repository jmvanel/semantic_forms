package deductions.runtime.data_cleaning

import org.w3.banana.{OWLPrefix, RDF}

import scala.language.postfixOps


trait PropertiesCleaner[Rdf <: RDF, DATASET]
extends BlankNodeCleanerBase[Rdf, DATASET] {
  import ops._

//  val namedGraphForTrackingDuplicates = URI("urn:duplicates")
  private val owl = OWLPrefix[Rdf]

  /**
   *  keep track of the merge of triples with owl:equivalentProperty or owl:sameAs
   *  includes transaction
   */
  def processKeepingTrackOfDuplicates(
      uriTokeep: Rdf#Node,
      duplicateURIs: Seq[Rdf#Node],
      auxiliaryOutput : Rdf#MGraph = makeEmptyMGraph()
  ): Unit = {
//	  println(s"processKeepingTrackOfDuplicates: uriTokeep $uriTokeep duplicateURIs ${duplicateURIs.mkString(", ")}")
    if (uriTokeep != URI("")) {
      // check that it's actually a property
      val transaction = rdfStore.rw(dataset, {
        val equivalenceProperty =
          if (isProperty(uriTokeep))
            owl.equivalentProperty
          else if (isClass(uriTokeep))
            owl.equivalentClass
          else
            owl.sameAs
        val pgs = for { duplicateURI <- duplicateURIs } yield {
          duplicateURI -- equivalenceProperty ->- uriTokeep
        }
        val graph = pgs.foldLeft(emptyGraph)((x, y) => { x union y.graph })
//        rdfStore.appendToGraph(dataset, namedGraphForTrackingDuplicates, graph)
        addTriples( auxiliaryOutput, graph.triples )
      })
//      println(s"processKeepingTrackOfDuplicates 3: transaction $transaction" )
    }
  }
  
}
