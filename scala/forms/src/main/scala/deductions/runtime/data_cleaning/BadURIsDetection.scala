package deductions.runtime.data_cleaning

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.utils.RDFStoreLocalProvider
import deductions.runtime.utils.{DefaultConfiguration, RDFHelpers, URIManagement}
import org.w3.banana.RDF

/** See also [[FixBadURIApp]] */
trait BadURIsDetection[Rdf <: RDF, DATASET]
    extends RDFStoreLocalProvider[Rdf, DATASET]
    with RDFHelpers[Rdf]
    with URIManagement {

  import ops._

  /** */
  def listBadURIs() = {
//    val unionGraphURI = makeUri("urn:x-arq:UnionGraph")
//    val allNamedGraph = rdfStore.getGraph(dataset, unionGraphURI).get
    println(s"allNamedGraph.size ${allNamedGraph.size}")

    var count = 0

    def processBadTriples[T](func: Rdf#Triple => T) =
      for (
        badTriple <- find(allNamedGraph, ANY, ANY, ANY)
      ) {
        val objet = foldNode(badTriple.objectt)(
          uri => nodeToString(uri), bn => "", lit => "")
        if (notAcceptable(badTriple.subject) ||
          notAcceptable(badTriple.objectt)) {
          func(badTriple)
        }
      }

    processBadTriples { badTriple =>
      println(s"bad triple: $badTriple")
      count += 1
    }
    println(s"Triples with bad URIs: count $count")

//    val mgraph = makeMGraph(unionGraphURI, dataset)
//    processBadTriples { badTriple =>
//      removeTriple(mgraph, badTriple)
//      println(s"removed Triple $badTriple")
//    }
//    println(s"After: allNamedGraph.size ${allNamedGraph.size}")
  }

  private def notAcceptable(node: Rdf#Node) = {
    val uriString = foldNode(node)(
      uri => nodeToString(uri), bn => "", lit => "")
    !isCorrectURI(uriString)
  }
}

object BadURIsDetectionApp extends ImplementationSettings.RDFCache
    with App
    with BadURIsDetection[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {
  val config = new DefaultConfiguration {
    override val needLoginForEditing = true
    override val needLoginForDisplaying = true
    override val useTextQuery = false
  }
  listBadURIs()
}