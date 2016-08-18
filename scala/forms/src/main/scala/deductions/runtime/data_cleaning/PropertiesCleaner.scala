package deductions.runtime.data_cleaning

import org.w3.banana.RDF
import deductions.runtime.dataset.RDFStoreLocalProvider
import deductions.runtime.services.SPARQLHelpers
import org.w3.banana.jena.JenaModule
import deductions.runtime.jena.RDFStoreLocalJenaProvider
import scala.language.postfixOps
import org.w3.banana.RDFPrefix
import org.w3.banana.syntax._
import org.w3.banana.diesel._

import deductions.runtime.utils.RDFHelpers
import deductions.runtime.dataset.RDFOPerationsDB
import org.w3.banana.OWLPrefix
import org.w3.banana.PointedGraphs
import org.w3.banana.RDFSPrefix


trait PropertiesCleaner[Rdf <: RDF, DATASET]
extends BlankNodeCleanerBase[Rdf, DATASET] {
  import ops._
  import rdfStore.graphStoreSyntax._
  import rdfStore.transactorSyntax._

//  val namedGraphForTrackingDuplicates = URI("urn:duplicates")
  private val owl = OWLPrefix[Rdf]
  private val rdfs = RDFSPrefix[Rdf]

  /**
   *  keep track of the merge of triples with owl:equivalentProperty or owl:sameAs
   *  includes transaction
   */
  def processKeepingTrackOfDuplicates(
      uriTokeep: Rdf#URI,
      duplicateURIs: Seq[Rdf#URI],
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
